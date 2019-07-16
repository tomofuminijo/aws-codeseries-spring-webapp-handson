# AWS Code シリーズを利用してJava Spring Boot アプリケーションをEC2に自動デプロイするハンズオン

Java Spring Boot Web App を利用して、CodeCommit、CodeBuild、CodeDeploy、CodePipeline の利用方法の概要を学びます。

# 実行環境

このハンズオンは、AWS Cloud9 上で実行する前提できししていますが、どの環境でも動かせます。（OpenJDK 8 およびMaven があれば良いです。）

# 事前準備

- IAM ポリシー  
  - IAM ユーザには以下のポリシーが必要になります
    - AmazonDynamoDBFullAccess
    - AWSCodeCommitFullAccess
    - AWSCodeBuildAdminAccess
    - AWSCodeDeployFullAccess
    - AWSCloud9Administrator
- Cloud9 の起動
  - 適当なリージョンでCloud9 を起動します。

# Step1: JDK 8 のインストール
ここでは、[Amazon Corretto (Java8)](https://aws.amazon.com/jp/corretto/)　をインストールします。   
まず、Cloud9 上でターミナルウィンドウを開き、以下のコマンドを実行します。

```
wget https://d2znqt9b1bc64u.cloudfront.net/java-1.8.0-amazon-corretto-devel-1.8.0_202.b08-2.x86_64.rpm

sudo yum install -y java-1.8.0-amazon-corretto-devel-1.8.0_202.b08-2.x86_64.rpm
```

JAVA_HOME 環境変数を設定しておきます。

```
echo "export JAVA_HOME=/usr/lib/jvm/java-1.8.0-amazon-corretto/" >> ~/.bash_profile

source ~/.bash_profile
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
    java -jar target/my-greeting-web-0.1.0.jar --spring.profiles.active=dev
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

# Step4: CodeCommit

1. リポジトリの作成

    ```
    aws codecommit create-repository --repository-name MyHandsOn --repository-description "My Handson repository" 
    ```
    
    以下のような情報が返却されます。
  
    ```
    {
      "repositoryMetadata": {
          "repositoryName": "MyHandsOn", 
          "cloneUrlSsh": "ssh://git-codecommit.your_region_code.amazonaws.com/v1/repos/MyHandsOn", 
          "lastModifiedDate": 1563268366.672, 
          "repositoryDescription": "My Handson repository", 
          "cloneUrlHttp": "https://git-codecommit.your_region_code.amazonaws.com/v1/repos/MyHandsOn", 
          "creationDate": 1563268366.672, 
          "repositoryId": "xxxxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxx", 
          "Arn": "arn:aws:codecommit:your_region_code:your_account_id:MyHandsOn", 
          "accountId": "your_account_id"
      }
    }
    ```
    
    上記の"cloneUrlHttp" の内容をコピーしておきます。

1. CodeCommit 認証情報ヘルパーの設定

    以下のコマンドを実行します。
    
    ```
    git config --global credential.helper '!aws codecommit credential-helper $@'
    git config --global credential.UseHttpPath true
    ```
    
    - 参考URL: [AWS CLI 認証情報ヘルパーを使用する Linux, macOS, or Unix での AWS CodeCommit リポジトリへの HTTPS 接続のセットアップステップ - AWS CodeCommit](https://docs.aws.amazon.com/ja_jp/codecommit/latest/userguide/setting-up-https-unixes.html)

1. CodeCommit リポジトリへのpush

    以下のコマンドを実行します。
    
    ```
    git init
    git add .
    git commit -m "My First Commit"
    git remote remove origin
    git remote add origin https://git-codecommit.your_region_code.amazonaws.com/v1/repos/MyHandsOn
    git push origin master
    ```

1. CodeCommit リポジトリに確認
    - マネジメントコンソールにアクセスしてCodeCommit リポジトリの内容を確認してみましょう


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
      - リポジトリ： MyHandsOn
    - 環境
      - 環境イメージ： マネージド型イメージ (デフォルト)
      - オペレーティングシステム: Ubuntu
      - ランタイム: Standard
      - イメージ： aws/codebuild/standard:2.0
    - アーティファクト
      - タイプ： Amazon S3
      - バケット名: 先程作成したバケットを選択します
      - セマンティックバージョニングの有効化: チェックを入れる
      - アーティファクトのパッケージ化: Zip にチェックを入れる
      - 追加設定
        - キャッシュタイプ: ローカル
        - SourceCache: チェックオン
        - CustomCache: チェックオン

5. "ビルドプロジェクトを作成する" ボタンをクリックします。

6. MyCodeBuild というビルドプロジェクトが正常に作成されたら、"ビルドの開始" ボタンをクリックします。

7. 「ビルドの開始」画面では、全てデフォルトのままで再度"ビルドの開始" ボタンをクリックします。

8. ビルド処理は初回は５分ほど時間がかかります。

9. ビルド処理が正常に終了したら、S3 にアーティファクトが出力されていることを確認します。
  - MyCodeBuild/my-greeting-web-0.1.0.jar が出力されていることを確認します


# Step6: CodeDeploy

  今回はCodeDeploy を利用してSpring Boot アプリをEC2 上にデプロイします。

## EC2 用のIAM ロールの作成

  - 以下のポリシーをアタッチしたEC2 用のIAMロールを作成します。
    - AmazonEC2RoleforAWSCodeDeploy
    - AmazonDynamoDBFullAccess

## EC2 の起動

1. EC2 インスタンスを起動します。以下の内容を指定してください。
    - AMI: Amazon Linux 2
    - IAM ロール: 先ほど作成したIAMロール
    - ユーザデータは以下を指定する
        
        ```
        #!/bin/bash
        yum -y update
        yum install -y ruby jq
         
        # install CodeDeploy Agent
        REGION=$(curl -s 169.254.169.254/latest/dynamic/instance-identity/document | jq -r ".region")
        cd /home/ec2-user
        wget https://aws-codedeploy-${REGION}.s3.amazonaws.com/latest/install
        chmod +x ./install
        ./install auto
        
        # install jdk
        wget https://d3pxv6yz143wms.cloudfront.net/8.212.04.2/java-1.8.0-amazon-corretto-devel-1.8.0_212.b04-2.x86_64.rpm
        sudo yum install -y java-1.8.0-amazon-corretto-devel-1.8.0_212.b04-2.x86_64.rpm
        ```
    - タグに、Name/MyHandsOnTarget を設定しておく
    - セキュリティグループは 8080 ポートを開けておく


## CodeDeploy の構成


1. CodeDeploy 用のサービスロールを作成します。
    - 対象サービス：CodeDeploy、ユースケース：CodeDeploy
    - 以下のポリシーを関連付けます(デフォルトで設定済み)
    - AWSCodeDeployRole

2. マネージメントコンソールでCodeDeploy サービス画面を表示します。

3. アプリケーション -> "アプリケーションの作成" ボタンをクリックします。

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

ここまででCodeCommit/CodeBuild/CodeDeploy の構成ができましたが、まだ各サービスが単体で道才している状態です。
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
    - ブランチ名: master

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
    git push origin master
    ```

4. マネジメントコンソールのCodePipeline サービス画面にて、パイプラインが動作していることを確認します。

5. Pipeline が正常に動作したら、実際にEC2 インスタンスにアクセスして新しいバージョンに変わっていることを確認します。

以上です。
