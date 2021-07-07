# AWS Code シリーズを利用してJava Spring Boot アプリケーションをEC2に自動デプロイするハンズオン

Java Spring Boot Web App を利用して、CodeCommit、CodeBuild、CodeDeploy、CodePipeline の利用方法の概要を学びます。

# 実行環境

このハンズオンは、AWS Cloud9 上で実行する前提で記載していますが、どの環境でも動かせます。（OpenJDK 11 およびMaven があれば良いです。）

# 事前準備

- IAM ポリシー  
  - IAM ユーザには以下のポリシーが必要になります
    - AmazonDynamoDBFullAccess
    - AWSCodeCommitFullAccess
    - AWSCodeBuildAdminAccess
    - AWSCodeDeployFullAccess
    - AWSCloud9Administrator
- Cloud9 の起動
  - 東京リージョンにて、Platform を"Amazon Linux 2" を指定してCloud9 を起動します。インスタンスタイプは、"t3.small" 以上を推奨します。

# Step1: JDK 11 のインストール
ここでは、[Amazon Corretto](https://aws.amazon.com/jp/corretto/)　をインストールします。   
まず、Cloud9 上でターミナルウィンドウを開き、以下のコマンドを実行します。

```
wget https://corretto.aws/downloads/latest/amazon-corretto-11-x64-linux-jdk.rpm

sudo rpm -ihv /home/ec2-user/environment/amazon-corretto-11-x64-linux-jdk.rpm
```

# Step2: Maven のインストール

ターミナル上で以下のコマンドを実行します。

```
sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
sudo yum install -y apache-maven
```

- 参考URL : [Maven を使用して設定する](https://docs.aws.amazon.com/ja_jp/cloud9/latest/user-guide/sample-java.html#sample-java-sdk-maven)

# Step3: ローカル環境で実行(git clone、ビルド、実行)

- ローカルbuild 処理
    ```
    git clone https://github.com/tomofuminijo/aws-codeseries-spring-webapp-handson.git

    cd aws-codeseries-spring-webapp-handson
    mvn package 
    ```

- ローカル環境でアプリケーション実行
  - DynamoDB ローカルの実行
    ```
    docker run -p 8000:8000 amazon/dynamodb-local
    ```
  - Java の実行
    ```
    java -jar target/my-greeting-web-1.0.0.jar --spring.profiles.active=dev
    ```

- ブラウザからアクセス   
  アプリケーションを実行すると以下のようなメッセージがターミナル右上に表示されます。   
    ```
    Cloud9 Help
    Your code is running at https://xxxxxxxxxxxxx.vfs.cloud9.us-east-1.amazonaws.com
    ```

    表示されたリンクをクリックするとアプリケーションにアクセスできます。

- アプリの動作確認  
    1. まだ必要なテーブルやデータがDynamoDB 上に無いので、まず最初に表示された画面の"Greeting" ボタンの下の **init** リンクをリクックします。  
    何もエラー画面が表示されなければ正常に動作しています。

    1. "Language" の入力欄に "ja" や"fr"、"ko" などと入力して"Greeting" ボタンを押すと、それぞれの言語にHello が表示されます。データはDynamoDB　から取得されます。
    
    1. 以下のコマンドを叩くことで、DynamoDB ローカル上にテーブルが作成されていることを確認できます。
    
        ```
        aws dynamodb list-tables --endpoint-url http://localhost:8000 --region localtest
        ```

    1. 動作確認が終わったらすべてのプロセスを終了します。



# Step4: CodeCommit

1. リポジトリの作成

    ターミナルで以下のコマンドを実行し、CodeCommit 上にリポジトリを作成します。

    ```
    aws codecommit create-repository --repository-name MyCodeSeriesHandsOn --repository-description "My Handson repository" 
    ```
    
    以下のような情報が返却されます。
  
    ```
    {
      "repositoryMetadata": {
          "repositoryName": "MyCodeSeriesHandsOn", 
          "cloneUrlSsh": "ssh://git-codecommit.ap-northeast-1.amazonaws.com/v1/repos/MyCodeSeriesHandsOn", 
          "lastModifiedDate": 1563268366.672, 
          "repositoryDescription": "My Handson repository", 
          "cloneUrlHttp": "https://git-codecommit.ap-northeast-1.amazonaws.com/v1/repos/MyCodeSeriesHandsOn", 
          "creationDate": 1563268366.672, 
          "repositoryId": "xxxxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx", 
          "Arn": "arn:aws:codecommit:ap-northeast-1:your_account_id:MyCodeSeriesHandsOn", 
          "accountId": "your_account_id"
      }
    }
    ```
    
    上記の"cloneUrlHttp" の内容をコピーしておきます。

1. (Option) git-remote-codecommit のインストール

    Cloud9 環境以外で実施している場合は、以下のURL を参考にgit-remote-codecommit をインストールしてください。
    
    - 参考URL: [git-remote-codecommit を使用して AWS CodeCommit への HTTPS 接続をセットアップする手順 - AWS CodeCommit](https://docs.aws.amazon.com/ja_jp/codecommit/latest/userguide/setting-up-git-remote-codecommit.html)

1. CodeCommit リポジトリへのpush

    以下のコマンドを実行します。
    
    ```
    git add .
    git commit -m "My First Commit"
    git remote remove origin
    git remote add origin codecommit::ap-northeast-1://MyCodeSeriesHandsOn
    git push origin main
    ```

1. CodeCommit リポジトリに確認
    - マネジメントコンソールにアクセスしてCodeCommit リポジトリの内容を確認してみましょう
        ```
        https://ap-northeast-1.console.aws.amazon.com/codesuite/codecommit/repositories/MyCodeSeriesHandsOn/browse?region=ap-northeast-1
        ```


# Step5: CodeBuild

CodeBuild を利用すると、多数の開発者がCodeCommit にコミットしたソースを指定したビルド手順で自動的にビルドすることができるよになります。
Build のためのサーバなどは管理不要です。

1. マネージメントコンソールにアクセスします。

2. サービスからS3 を選択します。

3. "バケットを作成する" ボタンを押して任意のバケットを作成します。

4. 作成したバケットのバージョニングを有効化します。

2. サービスからCodeBuild を選択します。

3. "ビルドプロジェクトを作成する" ボタンを押します。

4. 「ビルドプロジェクトを作成する」画面にて、以下の内容を入力します。指示していない項目に関してはデフォルトのままとします。
    - プロジェクト名: MyCodeBuild
    - 送信元
      - ソースプロバイダ： AWS CodeCommit (デフォルト)
      - リポジトリ： MyCodeSeriesHandsOn
    - 環境
      - 環境イメージ： マネージド型イメージ (デフォルト)
      - オペレーティングシステム: Ubuntu
      - ランタイム: Standard
      - イメージ： aws/codebuild/standard:5.0
    - アーティファクト
      - タイプ： Amazon S3
      - バケット名: 先程作成したバケットを選択します
      - アーティファクトのパッケージ化: Zip にチェックを入れる
      - 追加設定
        - キャッシュタイプ: Amazon S3
        - キャッシュバケット: 先程作成したバケットを選択します
        - キャッシュパスのプレフィックス - オプショナル: cache
        - キャッシュのライフサイクル (日) - オプショナル: "1" を入力した後に、"+ 有効期限を追加する" ボタンをクリックする -> S3 上に自動的にライフサイクルが設定される

5. "ビルドプロジェクトを作成する" ボタンをクリックします。

6. MyCodeBuild というビルドプロジェクトが正常に作成されたら、"ビルドの開始" ボタンをクリックします。

7. 「ビルドの開始」画面では、全てデフォルトのままで再度"ビルドの開始" ボタンをクリックします。

8. ビルド処理は初回は５分ほど時間がかかります。

9. ビルド中にCodeBuild の画面にて、ビルドログなどでビルドの状況を確認できます。

10. ビルド処理が正常に終了したら、指定したS3 バケットにアーティファクトが出力されていることを確認します。
  - MyCodeBuild が出力されていることを確認します


# Step6: CodeDeploy

  今回はCodeDeploy を利用してSpring Boot アプリをEC2 上にデプロイします。

## EC2 用のIAM ロールの作成

  - 以下のAWS 管理ポリシーをアタッチしたEC2 用のIAMロールを作成します。
    - AmazonEC2RoleforAWSCodeDeploy
    - AmazonDynamoDBFullAccess
    - AmazonSSMManagedInstanceCore

## EC2 の起動

1. EC2 インスタンスを起動します。以下の内容を指定してください。
    - AMI: Amazon Linux 2
    - インスタンスタイプ: t2.micro
    - ネットワーク： Default VPC を利用
    - IAM ロール: 先ほど作成したIAMロール
    - ユーザデータは以下を指定する
        
        ```
        #!/bin/bash
        yum -y update
        
        # install jdk
        wget https://corretto.aws/downloads/latest/amazon-corretto-11-x64-linux-jdk.rpm
        sudo rpm -ihv amazon-corretto-11-x64-linux-jdk.rpm
        ```
    - タグに、Name/MyHandsOnTarget を設定しておく
    - セキュリティグループは 8080 ポートを開けておく


## CodeDeploy の構成


1. IAM ロールにて、CodeDeploy 用のサービスロールを作成します。
    - 対象サービス：CodeDeploy、ユースケース：CodeDeploy
    - 以下のポリシーを関連付けます(デフォルトで設定済み)
      - AWSCodeDeployRole

2. マネージメントコンソールでCodeDeploy サービス画面を表示します。

3. ナビゲーションペインにて、アプリケーション をクリックし、右側の画面にて "アプリケーションの作成" ボタンをクリックします。

4. 以下を入力します。
    - アプリケーション名: myhanodson-app
    - コンピューティングプラットフォーム: EC2/オンプレミス

5. "アプリケーションの作成" ボタンをクリックします。

6. 「デプロイグループ」タグの"デプロイグループの作成" ボタンをクリックします。

7. 以下を入力します。指定されていないものはデフォルトのままとします。
    - デプロイグループ名: myhandson-deploygroup
    - サービスロール: 手順１で作成したIAMロール
    - 環境設定
        - Amzon EC2 インスタンス: チェック
        - タググループのキー: Name
        - 値：MyHandsOnTarget
    - ロードバランサー
        - ロードバランシングを有効にする：チェックを外す
8. "デプロイグループの作成"ボタンをクリックします。

9. 「myhandson-deploygroup」画面にて、"デプロイの作成" ボタンをクリックします。

10. 「Create deployment」 画面にて以下を入力します。
    - デプロイグループ：myhandson-deploygroup
    - リビジョンタイプ: "アプリケーションはAmazon S3 に格納されています" にチェック(デフォルト)
    - リビジョンの場所: s3://your_code_build_artifact_bucket/MyCodeBuild
    - リビジョンファイルの種類: .zip 

11. "デプロイの作成" ボタンをクリックします。

12. デプロイが作成されるとともに、デプロイが開始されます。

13. 正常にデプロイが終わったら、EC2 にブラウザからアクセスします。
    - EC2 のパブリックIP を取得してブラウザで8080 ポートでアクセスしてみる
    - ローカル環境と同じ画面が表示されることを確認する


# Step7: CodePipeline の構成

ここまででCodeCommit/CodeBuild/CodeDeploy の構成ができましたが、まだ各サービスが単体で動いている状態です。
CodeCommit にコードがPush されたら、CodeBuild/CodeDeploy まで自動的に実行されるようにCodePiplelineを構成します。

1. マネージメントコンソールでCodePipeline を選択します。

2. "パイプラインを作成する" ボタンをクリックします。

3. 以下の値を入力します。
    - パイプライン名: myhandson-pipeline
    - サービスロール: 新しいサービスロール(デフォルト)

4. "次に" ボタンをクリックします。

5. 「ソースステージを追加する」 画面にて、以下を入力します。
    - ソースプロバイダー: CodeCommit
    - リポジトリ名: MyHandsOn
    - ブランチ名: main

6. "次に" ボタンをクリックします。

7. 「ビルドステージを追加する」画面にて、以下を入力します。
    - プロバイダーを構築する: AWS CodeBuild
    - プロジェクト名: MyCodeBuild

8. "次に" ボタンをクリックします。

9. 「デプロイステージを追加する」画面にて、以下を入力します。
    - デプロイプロバイダー: AWS CodeDeploy
    - アプリケーション名: myhandson-app
    - デプロイグループ: myhandson-deploygroup
10. "次に" ボタンをクリックします。

11. 「レビュー」画面にて、"パイプラインを作成する" ボタンをクリックします。

12. Pipeline が作成されるとすぐに、現在の最新のリビジョンでパイプラインが実行されます。

## CodeCommit にgit push

CodeCommit にgit push してPipeline が自動的に実行されることを確認します。

1. Cloud9 にて、src/main/resources/templates/index.html を開きます。

2. "Hello! Ver: 1.0.0" となっているところを "Hello! Ver: 2.0.0" に変更します。

3. Cloud9 のターミナルで、以下のコマンドを実行してリポジトリにコードを push します
    ```
    git add .
    git commit -m "ver 2.0.0"
    git push origin main
    ```

4. マネジメントコンソールのCodePipeline サービス画面にて、パイプラインが動作していることを確認します。

5. Pipeline が正常に動作したら、実際にEC2 インスタンスにアクセスして新しいバージョンに変わっていることを確認します。

以上です。
