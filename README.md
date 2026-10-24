# AWS
Amazon Web Sevice test project


Generate banner -  http://patorjk.com/software/taag

#### How run app (e.g. task-01):

Secrets are in .env


```bash
  AWS_PROFILE=default \
  AWS_REGION=eu-north-1 \
  AWS_S3_BUCKET=my-bucket-118697848787-eu-north-1-an \
  AWS_SQS_QUEUE_URL=https://sqs.eu-north-1.amazonaws.com/118697848787/document-jobs \
  AWS_ACCESS_KEY_ID=my_key \
  AWS_SECRET_ACCESS_KEY=my_secret \
  ./gradlew task-01:bootRun

```
