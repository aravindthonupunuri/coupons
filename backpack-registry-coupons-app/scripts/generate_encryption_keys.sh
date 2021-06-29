#!/usr/bin/env bash

set -e

# generate private key for encryption/decryption via openssl
scriptname="generate_encryption_keys"
. `dirname $0`/appinfo.sh
. $scriptDir/logging.sh
. $scriptDir/utils.sh

usage="./${scriptname}.sh <environment | $SUPPORTED_ENVIRONMENTS>"

echo " "
echo "Running: $0 $@"
echo " "

check_environment "$1"
envname=$1

cd $scriptDir

# set filenames to use
privkey_file=/tmp/${appname}_privatekey_${envname}.pem
privkey_file_base64=/tmp/${appname}_privatekey_${envname}_base64.txt

# remove existing keys for the env, if any
rm -f $privkey_file
rm -f $privkey_file_base64

echo " "
log_msg "Generating Private Key: $privkey_file"
openssl genrsa -out $privkey_file 2048

echo " "
log_msg "** Base64 Encoding Private Key with _base64.txt extension: $privkey_file_base64"
cat $privkey_file|openssl base64|tr -d '\n' > $privkey_file_base64

echo " "
log_msg "Base64 Private Key:"
cat $privkey_file_base64
echo " "
log_msg "Done..."
cd $currDir
