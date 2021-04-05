
### App to scan files on S3 with ClamAV virus scanner using AWS Lambda deployed in Docker Container

#### Build image
```
mvn compile dependency:copy-dependencies -DincludeScope=runtime
docker build -t clamav-lambda .
```
#### Run local
```
docker run -e s3dstSecretKey=qIumciMYjK9XVsfh7dLo/XmrVHj8ja5uCf3U5PR4 -e s3dstAccessKey=AKIARZH42LGNA2WGP6LY --name=lambda -p 9009:8080 clamav-lambda biz.cits.clamav.lambda.handler.Update
curl -XPOST "http://localhost:9009/2015-03-31/functions/function/invocations" -d '{"task":"update"}'
docker cp src/test/resources/eicar/ lambda:/var/task/eicar
curl -XPOST "http://localhost:9009/2015-03-31/functions/function/invocations" -d '{"task":"scan", "file":"eicar"}'
```
#### Push to AWS
```
docker tag clamav-lambda:latest 122936777114.dkr.ecr.us-east-1.amazonaws.com/clamav-lambda:latest
docker push 122936777114.dkr.ecr.us-east-1.amazonaws.com/clamav-lambda:latest
```

#### Env Variables
```
storeBucket=clamav-out
dstBucket=clamav-out
```
