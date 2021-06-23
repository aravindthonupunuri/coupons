#!/usr/bin/env bash

set -e

# decrypts an encrypted data file located under app's secret resources location folder
scriptname="decrypt_secret_config"
. `dirname $0`/appinfo.sh
. $scriptDir/logging.sh
. $scriptDir/utils.sh

usage="./${scriptname}.sh <input-file-name-with-path> <environment | $SUPPORTED_ENVIRONMENTS> <priv-key-file-base64 or gardenia or vela> <optional debug>"
# priv-key-file in pem format is already a base64 encoded file but with new lines in it.
# priv-key-file-base64 represents original pem file further base64 encoded to become a single line base64 encoded string
# priv-key-file-base64 filename needs to end with _base64.txt

# Encryption Process
# ==================
# We have private key  per application that is used to encrypt secret data to an encrypted-output-file
# which can be stored safely in git.

# Decryption Process
# ==================
# Read encrypted-output-file
# Using app-private-key, decrypt encrypted-file-data => decrypted-file-data
#

echo " "
log_msg "Running: $0 $@"
echo " "

filename_prefix=$appname
tmp_filename_prefix=${appname}decrypttmp

if [ -z "$1" ]; then
    log_usage_err "Missing input file name argument"
fi
file_to_decrypt="$1"
if [ ! -f "$file_to_decrypt" ]; then
    log_usage_err "Nonexisting input file name $file_to_decrypt"
fi

check_environment "$2"
envname=$2

privkey_file=/tmp/${tmp_filename_prefix}_privkey.pem
use_gardenia=false
use_vela=false
if [ -z "$3" ]; then
    log_usage_err "Missing private key file argument"
elif [ "$3" == "gardenia" ]; then
    use_gardenia=true
elif [ "$3" == "vela" ]; then
    use_vela=true
elif [[ ! "$3" =~ .*"_base64.txt"$ ]]; then
    log_usage_err "Invalid private Key File [without _base64.txt extension]: $3"
elif [ ! -f $3  ]; then
    log_usage_err "Nonexisting private key file: $privkey_file"
else
    # original privkey pem file was base64 encoded
    # decode privkey base64 encoded file to extract original privkey_file pem file
    cat "$3"|openssl base64 -d -A > $privkey_file
fi

debug=false
if [ ! -z "$4" ]; then
    if [ "$4" == "debug" ]; then
        echo " "
        log_msg "*** WARN: Enabling debug output of decrypted data"
        echo " "
        debug=true
    else
        log_usage_err "Invalid debug argument"
    fi
fi

privkey_name=${appname}-privkey-${envname}
if [ "$use_gardenia" == true ]; then
    # read private key from gardenia
    log_msg "Reading private key ${gitorg}/${gitrepo}/${privkey_name} from gardenia"
    privkey_file_base64_data=`gardenia read repo "$gitorg"/"$gitrepo" $privkey_name 2>&1`

    value_line=`grep 'value:' <<< "$privkey_file_base64_data"`

    privkey_base64=`echo ${value_line//value: /@}`
    privkey_base64=`echo $privkey_base64|cut -d'@' -f2`
    privkey_base64=`echo $privkey_base64|cut -d'"' -f1`

    echo $privkey_base64|openssl base64 -d -A > $privkey_file
elif [ "$use_vela" == true ]; then
    # read private key from enterprise-secrets for vela
    log_msg "Reading private key from enterprise-secrets"
    read_enterprise_secret_value ${envname} privkey_file_base64_data
    echo "$privkey_file_base64_data"|openssl base64 -d -A > $privkey_file
fi

cd $scriptDir

# read input data file, and separate its contents into into encrypted_data and encrypted_random_key (using space delimiter)
log_msg "Reading input data file: $file_to_decrypt"
encrypted_data=`cat $file_to_decrypt`

# decrypt the encrypted_data using private key
decrypted_output_file=/tmp/${filename_prefix}_decrypted_data.txt
if [ ! -z "$DECRYPTED_DATA_FILE" ]; then
    # variable DECRYPTED_DATA_FILE is set (as output filename) when called from deploy_tap_config.sh
    log_msg "Externally provided Decrypted Data Output file: $DECRYPTED_DATA_FILE"
    decrypted_output_file=$DECRYPTED_DATA_FILE
fi
echo " "
log_msg "Decrypting encrypted_data to: $decrypted_output_file"

# Openssl can base64 decode and decrypt in the same step with the -a or -base64 switch.
# But there is a bug in openssl's base64 processing, it expects a newline at the end of the base64 encoded data.
# This causes issues when the secret data input file is long (longer than 735 chars)
# Therefore decode the base64 first separately i.e. don't use -base64 flag with openssl decode (enc -d) command
echo -n $encrypted_data| openssl base64 -d -A | openssl enc -d -aes-256-cbc -out $decrypted_output_file -pass file:$privkey_file

#remove newline from end of file, if any (TAP doesn't like newline char at end of base64 coded secrets)
perl -pi -e 'chomp if eof' $decrypted_output_file

if [ "$debug" == true ]; then
    echo " "
    echo "=============================="
    log_msg "Decrypted data:"
    echo "=============================="
    cat $decrypted_output_file
fi

echo " "
echo "=============================="
log_msg "Cleaning up temp files:"
echo "=============================="
rm -f /tmp/${tmp_filename_prefix}*

cd $currDir
