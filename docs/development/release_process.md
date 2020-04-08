# Release Process

## Overview

This document outlines the procedure to release Deep Java Library (DJL) project to maven central. 

## Step 1: Preparing the Release Candidate

### Step 1.1 Publish javadoc to S3 bucket

Make sure you are using correct aws credential and run the following command:

```shell script
cd djl
./gradlew -Prelease uploadJavadoc
```

### Step 1.2: Bump up versions in documents to point to new url

Edit [README Release Notes section](../../README.md#release-notes) to add link to new release. 

Update build version with the following command:
```shell script
cd djl
./gradlew -PpreviousVersion=X.X.X iFV
```
Make a commit, get reviewed, and then merge it into github.

### Step 1.3: Upload javadoc-index.html to S3 bucket

```shell script
aws s3 cp website/javadoc-index.html s3://javadoc-djl-ai/index.html
```

### Step 1.4: Publish Native library to sonatype staging server

This step depends on if there is a new release for different engines.
If nothing changes between previous and current version, you don't need to do this step.

#### MXNet

Run the following command to trigger mxnet-native publishing job:
```shell script
curl -XPOST -u "USERNAME:PERSONAL_TOKEN" -H "Accept: application/vnd.github.everest-preview+json" -H "Content-Type: application/json" https://api.github.com/repos/awslabs/djl/dispatches --data '{"event_type": "mxnet-staging-pub"}'
```

#### PyTorch

Run the following command to trigger pytorch-native publishing job:
```shell script
curl -XPOST -u "USERNAME:PERSONAL_TOKEN" -H "Accept: application/vnd.github.everest-preview+json" -H "Content-Type: application/json" https://api.github.com/repos/awslabs/djl/dispatches --data '{"event_type": "pytorch-staging-pub"}'
```

### Step 1.5: Publish DJL library to sonatype staging server

Run the following command to trigger DJL publishing job:
```shell script
curl -XPOST -u "USERNAME:PERSONAL_TOKEN" -H "Accept: application/vnd.github.everest-preview+json" -H "Content-Type: application/json" https://api.github.com/repos/awslabs/djl/dispatches --data '{"event_type": "release-build"}'
```

### Step 1.6: Remove -SNAPSHOT in examples and jupyter notebooks

Run the following command with correct version value:
```shell script
cd djl
git clean -xdff
./gradlew release
git commit -a -m 'Remove -SNAPSHOT for release vX.X.X'
git tag -a vX.X.X -m "Releasing version vX.X.X"
git push origin vX.X.X
```

## Step 2: Validate release on staging server

Login to https://oss.sonatype.org/, and find out staging repo name.

Run the following command to point maven repository to staging server:
```shell script
cd djl
git checkout vX.X.X
./gradlew -PstagingRepo=aidjl-XXXX staging
```

### Step 2.1: Validate examples project are working fine

```shell script
cd examples
# By default it uses mxnet-engine
# Please switch to pytorch, tensorflow engine to make sure all the engines pass the test 
./gradlew run 
mvn package 
mvn exec:java -Dexec.mainClass="ai.djl.examples.inference.ObjectDetection"
```

### Step 2.2: Validate jupyter notebooks

Make sure jupyter notebook and running properly and all javadoc links are accessible.
```shell script
cd jupyter
jupyter notebook
```

## Step 3: Validate javadoc url in documents

Navigate to DJL github site, select tag created by earlier step, open markdown files and
check javadoc links are accessible. 

## Step 4: Publish staging package to maven central

Login to https://oss.sonatype.org/, close staging packages and publish to maven central.

Make sure all packages are available on maven central. It may take up to 2 hours. File a ticket
to sonatype if we run into CDN cache issue like: [this](https://issues.sonatype.org/browse/MVNCENTRAL-5470).

## Step 5 Create a Release Note

Once we confirmed release packages are all working, we can create a release note:

Navigate to DJL github site, select "Release" tab and click "Draft a new Release" button.
Select tag that created by previous step. Check "This is a pre-release" checkbox.

Release notes content should include the following:
- list of new features
- list of bug fixes
- limitations and known issues
- API changes and migration document

Once "Publish release" button is clicked, a github Action will be triggered, and release packages
will be published sonatype staging server.

## Step 6: Post release tasks

### Step 6.1: Upgrade version for next release

```shell script
cd djl
./gradlew -PtargetVersion=X.X.X iBV
```

Create a PR to get reviewed and merge into github.

### Step 6.2: Publish new snapshot to sonatype

Manually trigger a nightly build with the following command:
```shell script
curl -XPOST -u "USERNAME:PERSONAL_TOKEN" -H "Accept: application/vnd.github.everest-preview+json" -H "Content-Type: application/json" https://api.github.com/repos/awslabs/djl/dispatches --data '{"event_type": "nightly-build"}'
```
