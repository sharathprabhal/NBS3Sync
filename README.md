NBS3Sync
========

An auto-syncing noBackend cloud storage app using only Amazon S3, Amazon SNS and Amazon SQS

Go [here](http://sharathprabhal.tumblr.com/post/68737134239/a-nobackend-cloud-storage-app-using-only-aws) for more information.

##Build
```sh
mvn clean install
```
##Run

Create a config file with the following params

```
awsAccessKey=yourAwsAccessKey
awsSecretKey=yourAwsSecretKey
bucket=someBucketName
queueName=uniqueNameForEachQueue
baseDirectory=/path/to/folder/to/watch
```

Please note that the `bucket` should be consistent across clients and the `queueName` should be unique

Run the JAR as

```
java -jar ./target/NBS3sync-1.0-SNAPSHOT-r-with-dependencies.jar /path/to/propsfile.properties
```

###Requirements
This application requires JAVA 1.7


