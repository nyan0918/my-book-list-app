# 書籍管理アプリ

所有する書籍を管理するための Android アプリケーションです。ISBN のスキャンで書籍情報を取得し、ローカルデータベースに保存・一覧表示・詳細確認・削除ができます。外部 API（Google Books API）から書誌情報とカバー画像を取得して表示します。

## 主な機能

- ISBN の読み取りによる書籍検索（単体モード / 一括モード）
- 検索結果の確認後、ローカルデータベース（Room）へ保存
- 保存済み書籍の一覧表示、詳細表示
- 複数選択による一括削除

<img src="/GIF_20260129_233610_684.gif" width="350">

## アーキテクチャ概要

- **UI 層** : Activity・JetpackCompose
- **ViewModel 層** : UI 状態管理（`BookViewModel`）
- **ドメイン層** : UseCase（検索、保存、削除、詳細取得 など）
- **データ層** : Repository（`BookRepository`）、Room（`BookDao`）、外部APIクライアント（`GoogleBooksService`）

## 主たるソースコード

1. [MainActivity](app/src/main/java/com/example/mybooksapplication/MainActivity.kt)：アプリのエントリポイント。ナビゲーションホストの起動と権限処理を担当します。
2. [MyBooksApp](app/src/main/java/com/example/mybooksapplication/MyBooksApp.kt)：Composeを使った画面遷移とルートUIを定義します。
3. Screen
   * [BookListScreen](app/src/main/java/com/example/mybooksapplication/ui/screen/BookListScreen.kt)：書籍一覧画面
   * [ScanScreen](app/src/main/java/com/example/mybooksapplication/ui/screen/ScanScreen.kt)：バーコードスキャン画面
   * [BookDetailScreen](app/src/main/java/com/example/mybooksapplication/ui/screen/BookDetailScreen.kt)：書籍詳細画面
4. [BookViewModel](app/src/main/java/com/example/mybooksapplication/ui/viewmodel/BookViewModel.kt)：検索・保存・削除など UI に必要な状態とイベントを管理する ViewModel。
5. [BookRepository](app/src/main/java/com/example/mybooksapplication/data/BookRepository.kt)：データ取得の窓口。Room と Google Books API の間を仲介します。
6. [BookDao](app/src/main/java/com/example/mybooksapplication/data/local/BookDatabase.kt)：Room の DAO。エンティティの CRUD とクエリを提供します。永続化用エンティティの定義をします。
7. [GoogleBooksService](app/src/main/java/com/example/mybooksapplication/data/remote/GoogleBooksApi.kt)：Retrofit インターフェース。外部 API から書誌情報とカバー画像の URL を取得します。
8. [GoogleBooksDto](app/src/main/java/com/example/mybooksapplication/data/remote/GoogleBooksDto.kt):データオブジェクト
9. [di](app/src/main/java/com/example/mybooksapplication/di/AppModule.kt)：依存注入の設定（Retrofit、Room のバインド）。

<details>
<summary>もっと見る(クリック)</summary>

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
