# innove-gen-ai

### Install google cloud SDK

https://cloud.google.com/sdk/docs/install

 - download the tar file
 - run `./google-cloud-sdk/install.sh`
 - run `./google-cloud-sdk/bin/gcloud init`
 - (in a new terminal after it has all installed) run `gcloud auth application-default login`
 - run this command to get a bearer token to use with our services `gcloud auth print-access-token` (used as standard authorization header)

### Install intellij community edition
https://www.jetbrains.com/idea/ - Select and download the scala plugin once installed.

Open this project via intellij or using intellij's VCS (`git clone https://github.com/Innove-Gen-AI/innove-gen-ai`)

### Start the service via
- terminal - `sbt start`
- sbt shell - `start`

### Mongo
Install mongo https://www.mongodb.com/docs/v4.2/installation/#mongodb-community-edition-installation-tutorials Once installed start mongo on your machine.
