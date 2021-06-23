#!/usr/bin/env bash

set -e

# script to read/write app private key to enterprise-secrets as well as post as vela secret
scriptname="vela_secretkey"
. `dirname $0`/appinfo.sh
. $scriptDir/logging.sh
. $scriptDir/utils.sh

usage="
./${scriptname}.sh <command | read> <environment | $SUPPORTED_ENVIRONMENTS>
./${scriptname}.sh <command | write> <environment | $SUPPORTED_ENVIRONMENTS> <priv-key-file-base64>
"
# priv-key-file in pem format is already a base64 encoded file but with new lines in it.
# priv-key-file-base64 represents original pem file further base64 encoded to become a single line base64 encoded string
# priv-key-file-base64 filename needs to end with _base64.txt

echo " "
echo "Running: $0 $@"
echo " "

if [ -z "$1" ]; then
    log_usage_err "Missing command argument"
elif [[ "$1" != "list" && "$1" != "read" && "$1" != "write" ]]; then
    log_usage_err "Invalid command $1"
fi

check_environment "$2"
envname=$2
privkey_value_file=""

if [ "$1" == "write" ]; then
    if [ -z "$3" ]; then
        log_usage_err "Missing private Key File"
    elif [ ! -f "$3" ]; then
        log_usage_err "Nonexisting private Key File: $3"
    elif [[ ! "$3" =~ .*"_base64.txt"$ ]]; then
        log_usage_err "Invalid private Key File [without _base64.txt extension]: $3"
    fi
    privkey_value_file="$3"
fi

cd $scriptDir

echo " "
secret_name=`build_enterprise_secret_name envname`
if [ "$1" == "read" ]; then
    log_msg "Reading private key from enterprise-secrets"
    echo " "
    read_enterprise_secret_value ${envname} result
    echo $result
elif [ "$1" == "write" ]; then
    # warn if we are overwriting an existing private key
    set +e
    read_enterprise_secret_value ${envname} existing_key
    set -e
    echo "existing_key is $existing_key"
    if [[ "$existing_key" = null ]]; then
      log_err "Exiting due to missing vault policy secret $secret_name"
    fi
    if [[ "$existing_key" != *"404"* ]]; then
        proceed=""
        while [[ "$proceed" != "Y" && "$proceed" != "y" && "$proceed" != "N" && "$proceed" != "n" ]]; do
            echo " "
            read -p "WARNING: $secret_name already exists, do you want to overwrite? [Y/N]: " proceed
            if [[ "$proceed" == "N" || "$proceed" == "n" ]]; then
                echo "Exiting without overwriting..."
                exit 0
            fi
        done
    fi
    log_msg "Writing private key to enterprise-secrets"
    echo " "
    privkey_value=`cat $privkey_value_file`

    echo "write_enterprise_secret_value ${envname} $privkey_value"
    write_enterprise_secret_value ${envname} $privkey_value
    echo " "
fi
echo " "

cd $currDir
