# 緊急脱出MOD - 部位ヒット検知システム解説

## 概要

このMODでは、プレイヤーが受けたダメージを**頭・胴体・足**の3部位に振り分けています。
バニラのMinecraftにはダメージの着弾部位という概念がないため、独自の仕組みで判定しています。

---

## 判定の全体フロー

```
プレイヤーがダメージを受ける
        │
        ▼
LivingEntity.hurt() が呼ばれる
        │
        ▼
[Mixin] LivingEntityMixin.onHurt() が先に実行される
        │
        ├── ダメージソースの種類を判別
        │     ├── 飛び道具（Projectile）→ 弾の座標で判定
        │     ├── 近接攻撃（Entity）    → 攻撃者の視線でレイキャスト判定
        │     ├── 爆発等（位置あり）    → ソース位置からレイキャスト判定
        │     └── 環境ダメージ          → ダメージタイプ名で判定
        │
        ▼
判定結果を HitPositionTracker に一時保存（100ms有効）
        │
        ▼
[Forgeイベント] LivingDamageEvent が発火
        │
        ▼
EmergencyEscapeEventHandler.onLivingDamage()
        │
        ├── HitPositionTracker から判定結果を取得
        ├── 部位に応じてダメージ適用（HEAD/BODY/LEGS）
        └── バニラのHPダメージはキャンセル（event.setCanceled(true)）
```

---

## 判定の詳細

### 1. Mixin によるヒット情報の取得

**ファイル:** `mixin/LivingEntityMixin.java`

Minecraftの `LivingEntity.hurt(DamageSource, float)` メソッドの先頭にMixinでインジェクトしています。
ダメージが処理される**前**にヒット位置と部位を特定します。

```java
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "hurt", at = @At("HEAD"))
    private void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // ここでヒット判定を行う
    }
}
```

### 2. ダメージソース別の判定方法

#### (A) 飛び道具の場合（矢、トライデント、他MODの弾丸など）

```java
if (source.getDirectEntity() instanceof Projectile projectile) {
    // 弾の「現在の座標」を使ってどの部位にいるか判定
    Vec3 hitPosition = projectile.position();
    BodyPart bodyPart = BodyPartHitbox.getBodyPartAtPointWithPose(player, hitPosition);
}
```

**判定ロジック:**
- `source.getDirectEntity()` が `Projectile` を継承しているかチェック
- Projectileなら、その `position()`（= 弾がプレイヤーに当たった瞬間の座標）を取得
- その座標がプレイヤーのどの部位のヒットボックス内にあるかを判定

**ポイント:**
- 撃った人の視線方向ではなく、**弾自体の座標**で判定する
- 弾は放物線で飛ぶため、撃った人の視線とは一致しない
- **他MODの弾が `Projectile` を継承していない場合、この分岐に入らない**

#### (B) 近接攻撃の場合（剣、素手など）

```java
else if (source.getEntity() != null) {
    Vec3 attackOrigin = attacker.getEyePosition();    // 攻撃者の目の位置
    Vec3 attackDirection = attacker.getLookAngle();     // 攻撃者の視線方向
    BodyPart bodyPart = BodyPartHitbox.getHitBodyPartWithPose(player, attackOrigin, attackDirection);
}
```

**判定ロジック:**
- 攻撃者の目の位置から視線方向に10ブロックのレイ（光線）を飛ばす
- そのレイが各部位のヒットボックスと交差するかを `AABB.clip()` でテスト
- 最も近い交差のある部位を返す

#### (C) 爆発・位置ありダメージの場合

```java
else if (source.getSourcePosition() != null) {
    Vec3 sourcePos = source.getSourcePosition();
    Vec3 direction = playerCenter.subtract(sourcePos).normalize();
    BodyPart bodyPart = BodyPartHitbox.getHitBodyPartWithPose(player, sourcePos, direction);
}
```

#### (D) 環境ダメージの場合（落下、溶岩、窒息など）

```java
else {
    String damageType = source.type().msgId();  // "fall", "lava", "anvil" など

    if (damageType.contains("fall"))           → LEGS  // 落下 → 足
    if (damageType.contains("anvil"))          → HEAD  // 金床 → 頭
    if (damageType.contains("lava"))           → BODY  // 溶岩 → 胴体
    if (damageType.contains("lightningBolt"))  → HEAD  // 雷 → 頭
    // ... その他
}
```

---

## 3. ヒットボックスの定義

**ファイル:** `util/BodyPartHitbox.java`

MinecraftのHumanoidModelのソースコードにある正確なピクセル値を使用しています。

### モデル座標系

Minecraftのプレイヤーモデルは **16ピクセル = 1ブロック** の座標系です。
モデルのY=0は首の位置、Y=24が足の底です。

```
Y = -8  ┌────────┐  ← 頭のてっぺん
        │  HEAD  │     8×8×8ピクセル
Y =  0  ├────────┤  ← 首（モデルの原点）
        │  BODY  │     8×12×4ピクセル
        │        │
Y = 12  ├───┬┬───┤  ← 腰
        │LEG││LEG│     各4×12×4ピクセル
        │   ││   │
Y = 24  └───┘└───┘  ← 足の底
```

### ワールド座標への変換

```
変換式: worldY = player.getY() + (24 - modelY) / 32 × playerHeight
```

立ち状態（playerHeight = 1.8ブロック）の場合:

| 部位 | モデルY | ワールドY (足からの高さ) | 身長に対する% |
|------|---------|------------------------|-------------|
| **HEAD（頭）** | -8 〜 0 | 1.35 〜 1.8 | **75% 〜 100%** |
| **BODY（胴体）** | 0 〜 12 | 0.675 〜 1.35 | **37.5% 〜 75%** |
| **LEGS（足）** | 12 〜 24 | 0 〜 0.675 | **0% 〜 37.5%** |

しゃがみ状態（playerHeight = 1.5ブロック）では全て比例的にスケーリングされます。

### 各部位のヒットボックスサイズ（ワールド座標、立ち状態）

| 部位 | 高さ | 幅 | 奥行き | 中心からのオフセット |
|------|------|-----|--------|-------------------|
| HEAD | 0.45 | 0.5 | 0.5 | なし |
| BODY | 0.675 | 0.5 | 0.25 | なし |
| LEFT_ARM | 0.675 | 0.25 | 0.25 | X方向に+0.3125 |
| RIGHT_ARM | 0.675 | 0.25 | 0.25 | X方向に-0.3125 |
| LEGS（左右合体） | 0.675 | 0.24 | 0.25 | 各足X方向に±0.12 |

### ヒットボックスの判定メソッド

#### (a) ポイント判定: `getBodyPartAtPoint(player, hitPoint)`

飛び道具の着弾点がどの部位のAABB（軸平行バウンディングボックス）に含まれるかを調べます。

```java
// 各部位を0.05ブロック膨張させてチェック（端の判定漏れ防止）
AABB headBox = getBodyPartAABB(player, BodyPart.HEAD).inflate(0.05);
if (headBox.contains(hitPoint)) return HEAD;

AABB bodyBox = getBodyPartAABB(player, BodyPart.BODY).inflate(0.05);
if (bodyBox.contains(hitPoint)) return BODY;

// ... 腕、足も同様にチェック

// どのボックスにも入らなかった場合 → フォールバック（Y座標の割合で判定）
double relativeY = (hitPoint.y - player.getY()) / player.getBbHeight();
if (relativeY >= 0.75) return HEAD;
if (relativeY >= 0.375) return BODY;
return LEGS;
```

#### (b) レイキャスト判定: `getHitBodyPart(player, origin, direction)`

近接攻撃の場合、攻撃者の目線から視線方向にレイを飛ばし、各部位との交差をテストします。

```java
Vec3 rayEnd = attackOrigin.add(attackDirection.scale(10));  // 10ブロック先まで

// 各部位のAABBとレイの交差テスト
Optional<Vec3> headHit = headBox.clip(attackOrigin, rayEnd);
Optional<Vec3> bodyHit = bodyBox.clip(attackOrigin, rayEnd);
// ... 各部位

// 最も近い交差点の部位を返す
```

---

## 4. ポーズ対応（腕の振り、しゃがみ等）

### 仕組み

クライアント側でプレイヤーモデルの各パーツの回転角を定期的にサーバーに送信しています。

```
[クライアント]                        [サーバー]
ModelPartSyncHandler                PlayerModelPartCache
  │                                    │
  ├─ 5tickごとにモデルの               │
  │  回転角を取得                      │
  │      │                            │
  └─ SyncModelPartPacket で送信 ──→ キャッシュに保存
                                    （500ms有効）
                                       │
                                       ▼
                                 BodyPartHitbox の
                                 WithPose版メソッドで使用
```

### 送信データ

6パーツ × 6値 = 36個のfloat値:

| パーツ | データ |
|--------|-------|
| HEAD | x, y, z（位置オフセット）, xRot, yRot, zRot（回転ラジアン） |
| BODY | 同上 |
| LEFT_ARM | 同上 |
| RIGHT_ARM | 同上 |
| LEFT_LEG | 同上 |
| RIGHT_LEG | 同上 |

### 回転を考慮したヒットボックス

回転角に応じてAABBを拡張する近似的なアプローチ:

```java
// 回転がある場合、AABBを拡張して回転後のボリュームをカバー
double expandX = halfHeight * |sin(xRot)| + halfDepth * |sin(yRot)|;
double expandY = halfWidth  * |sin(zRot)| + halfDepth * |sin(xRot)|;
double expandZ = halfWidth  * |sin(yRot)| + halfHeight * |sin(zRot)|;
// 拡張されたAABBで判定
```

---

## 5. 部位の簡略化

最終的にダメージ計算では3部位に集約されます:

```
HEAD      → そのまま頭ダメージ
BODY      → 胴体ダメージ
LEFT_ARM  → 胴体ダメージとして扱う
RIGHT_ARM → 胴体ダメージとして扱う
LEGS      → ダメージなし（部位体力に影響しない）
```

---

## 6. フォールバック判定

Mixinでヒット情報が取れなかった場合のフォールバック処理があります。
（`EmergencyEscapeEventHandler.determineHitBodyPart()`内）

```java
// configで設定可能な閾値（デフォルト: 頭=75%, 胴=38%）
double headThreshold = playerY + playerHeight * (HEAD_THRESHOLD_PERCENT / 100.0);
double bodyThreshold = playerY + playerHeight * (BODY_THRESHOLD_PERCENT / 100.0);

// 飛び道具の場合
if (source.getDirectEntity() != null) {
    double hitY = source.getDirectEntity().getY();  // 弾のY座標
    if (hitY >= headThreshold) → HEAD
    else if (hitY >= bodyThreshold) → BODY
    else → LEGS
}

// 近接攻撃の場合
else if (source.getEntity() != null) {
    double hitY = attacker.getEyeY();  // 攻撃者の目の高さ
    hitY = clamp(hitY, playerY, playerY + playerHeight);
    if (hitY >= headThreshold) → HEAD
    else if (hitY >= bodyThreshold) → BODY
    else → LEGS
}
```

