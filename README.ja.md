# A11yHider

[English](README.md)

特定のアプリから「アクセシビリティサービスが一つも有効になっていない」ように
見せかける Xposed/LSPosed（Vector）モジュールです。アクセシビリティサービスが
有効になっていることを検知すると起動を拒否したり警告を出したりするアプリが
ありますが、そのサービスがランチャーのカスタマイズやテーマ関連など、
まったく無関係なものであっても引っかかることがあります。A11yHider は
フック対象のアプリから見て、アクセシビリティサービスが存在しないかのように
振る舞わせます。

## フックしている内容

スコープに登録したアプリに対して、実際の端末の状態に関係なく、以下の API
から固定値を返します。

| API | フック後の戻り値 |
|---|---|
| `AccessibilityManager.getEnabledAccessibilityServiceList()` | 空リスト |
| `AccessibilityManager.getInstalledAccessibilityServiceList()` | 空リスト |
| `AccessibilityManager.isEnabled()` | `false` |
| `AccessibilityManager.isTouchExplorationEnabled()` | `false` |
| `Settings.Secure.getString("enabled_accessibility_services")` | `""` |
| `Settings.Secure.getStringForUser(..., "enabled_accessibility_services")` | `""` |

特定のサービスだけを除外するのではなく「サービスは何もない」という固定値を
返す方式なので、後から別のアクセシビリティサービスを有効にして対象アプリが
それにも反応するようになった場合でも、そのまま機能し続けます。

## 必要なもの

- root 化済みの端末、かつ Xposed 互換のフレームワーク（LSPosed、または
  `VectorLegacyBridge` 経由でレガシー Xposed モジュールも実行できる
  Vector などのフォーク）。
- レガシー形式の Xposed モジュール（`xposed_init`、
  `IXposedHookLoadPackage`）のサポート。LSPosed/Vector はどちらも
  直接対応しています。

## インストール方法

1. APK をビルドするか（下記「ビルド方法」参照）、本リポジトリに
   添付されているビルド済みリリースを使用してください。
2. インストールします: `adb install a11yhider.apk`
3. LSPosed/Vector の管理アプリを開き、モジュール一覧から
   **A11yHider** を見つけて有効化してください。
4. モジュールのスコープ画面で、アクセシビリティサービスを隠したい
   アプリを追加してください。本モジュールは**デフォルトでスコープが
   空**の状態で配布されています。ここでアプリを追加するまでは
   何もフックされません。
5. 対象アプリを強制終了（または端末を再起動）し、次回起動時から
   フックが有効になるようにしてください。

対象アプリの追加・削除に再ビルドは不要です。スコープの管理はすべて
管理アプリの UI から行います。

> **初回有効化時は再起動が必要な場合があります。** 新規インストール
> 直後にモジュールを有効化し、対象アプリをスコープに追加しても、
> 端末を一度再起動するまで反映されないことがあります。これは、
> クリーンインストール直後（フレームワークにまだモジュールが
> 登録されていない状態）のテスト中に実際に発生した現象です。
> 既に有効化済みのモジュールに対するスコープの変更であれば、通常は
> 対象アプリを強制終了するだけで即座に反映されます。対象アプリを
> 強制終了してもアクセシビリティチェックが回避されない場合は、
> 一度再起動してから試してください。

## ビルド方法

このプロジェクトは意図的に Gradle に依存しておらず、Android SDK の
コマンドラインツールと JDK だけでビルドできます。

必要なもの:
- JDK 17
- `platforms/android-31` と `build-tools/31.0.0` を含む Android SDK
  （バージョンが異なる場合はパスを適宜調整してください）

```powershell
$JAVAC = "<jdk17>\bin\javac.exe"
$JAR = "<jdk17>\bin\jar.exe"
$JARSIGNER = "<jdk17>\bin\jarsigner.exe"
$ANDROID_JAR = "<sdk>\platforms\android-31\android.jar"
$AAPT = "<sdk>\build-tools\31.0.0\aapt.exe"
$D8 = "<sdk>\build-tools\31.0.0\d8.bat"

# 1. コンパイル
& $JAVAC -source 8 -target 8 -encoding UTF-8 -cp $ANDROID_JAR -d build\classes (Get-ChildItem src -Recurse -Filter *.java | % FullName)

# 2. モジュール自身のクラスだけを dex 化する（jp/... 配下。Xposed API のスタブは含めない）
& cmd /c $D8 --min-api 26 --output build (Get-ChildItem build\classes\jp -Recurse -Filter *.class | % FullName)

# 3. リソースとマニフェストをパッケージング
& $AAPT package -f -M AndroidManifest.xml -S res -I $ANDROID_JAR -F build\a11yhider.apk

# 4. dex を追加
Push-Location build; & $AAPT add a11yhider.apk classes.dex; Pop-Location

# 5. assets/xposed_init を追加する（スラッシュ区切りで、BOM なしにすること）
& $JAR uf build\a11yhider.apk assets/xposed_init

# 6. 署名する（ローカル/サイドロード用途であればデバッグキーストアで十分）
& $JARSIGNER -keystore debug.keystore -storepass android -keypass android build\a11yhider.apk debug
```

## プロジェクト構成

```
src/jp/a11yhider/HideAccessibilityServices.java   モジュール本体のロジック
src/jp/a11yhider/DummyActivity.java               ランチャー用アクティビティ（使い方の案内画面）
src/de/robv/android/xposed/...                    Xposed API のスタブ（コンパイル時のみ使用）
assets/xposed_init                                 フレームワークに読み込ませるクラス名
res/values/arrays.xml                              xposed_scope（空のままにしてあり、管理アプリのUIから設定する）
AndroidManifest.xml
```

## 免責事項

本モジュールは、フック対象のアプリが端末のアクセシビリティの状態について
観測できる内容を変更します。自分が所有する端末で、動作を変更する権限が
ある対象アプリに対してのみ使用してください。一部のアプリは、アクセシビ
リティのチェックを不正利用防止や改ざん検知のロジックの一部として利用して
いる場合があります。この検知を回避することは、それらのアプリの利用規約に
違反する可能性があります。

## ライセンス

MIT — [LICENSE](LICENSE) を参照してください。
