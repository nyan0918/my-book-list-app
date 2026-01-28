# 書籍管理アプリ

このリポジトリは、所有する書籍を管理するための Android アプリケーションです。ISBN のスキャンや手動入力で書籍情報を取得し、ローカルデータベースに保存・一覧表示・詳細確認・削除ができます。外部 API（Google Books API）から書誌情報とカバー画像を取得して表示します。

## 主な機能

- ISBN の読み取りによる書籍検索（単体モード / 一括モード）
- 検索結果の確認後、ローカルデータベース（Room）へ保存
- 保存済み書籍の一覧表示、詳細表示
- 複数選択による一括削除・一括保存

## アーキテクチャ概要

- UI 層: Activity・JetpackCompose
- ViewModel 層: UI 状態管理（`BookViewModel`）
- ドメイン層: UseCase（検索、保存、削除、詳細取得 など）
- データ層: Repository（`BookRepository`）、Room（`BookDao`）、外部APIクライアント（`GoogleBooksService`）

<details>
<summary>もっと見る</summary>

## 動作要件

- Android Studio（推奨）
- ネットワーク
- （オプション）Google Books API キーを `BuildConfig` に設定

## ローカルでのビルド・テスト（Windows PowerShell）

- ビルド（Debug APK 作成）:

```powershell
.\gradlew assembleDebug --no-daemon
```

- 単体テスト実行（app モジュールの debug 単体テスト）:

```powershell
.\gradlew :app:testDebugUnitTest --no-daemon
```

- Jacoco カバレッジレポート（プロジェクトに設定がある場合）:

```powershell
.\gradlew :app:jacocoTestReport
```

> 注意: JVM 単体テスト内で Android API（例: `android.util.Log`）を呼ぶとエラーになることがあります。既にテストルール（例: `MockLogRule`）でモック化する対応が入っています。

</details>

## テストについて

- 単体テストは `app/src/test` にあります。JUnit、MockK、kotlinx-coroutines-test を使って ViewModel や Repository のユニットテストを実装しています。
