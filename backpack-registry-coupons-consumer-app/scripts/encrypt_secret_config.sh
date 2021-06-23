#!/usr/bin/env bash

set -e

# encrypts input text file data (input file name shouldn't have any environment extension in it)
# output filename is derived from input data filename extended with environment
# e.g. input file name: secret.yml => output file name: secret-dev.yml for dev environment
scriptname="encrypt_secret_config"
. `dirname $0`/appinfo.sh
. $scriptDir/logging.sh
. $scriptDir/utils.sh

usage="./${scriptname}.sh <input-data-file-with-path> <environment | $SUPPORTED_ENVIRONMENTS>"

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
echo "Running: $0 $@"
echo " "

if [ -z "$1" ]; then
    log_usage_err "Missing input data file argument"
fi
file_to_encrypt=$1
if [ ! -f $file_to_encrypt ]; then
    log_usage_err "Nonexisting input data file: $file_to_encrypt"
fi

check_environment "$2"
envname=$2

cd $scriptDir

filename_prefix=${appname}file
tmp_filename_prefix=${appname}encrypttmp

# encrypted data output file

# encrypt the secret-config data file
# extract just the file name out of file_to_encrypt
input_filename_part=`echo $file_to_encrypt | awk -F "/" '{print $NF}'`
input_file_name=`echo $input_filename_part | awk -F "." '{print $1}'`
input_file_ext=`echo $input_filename_part | awk -F "." '{print $2}'`

# define the name of output file using input_file_name with path under secret resources_location
encrypted_data_outfile=${secret_resources_location}/$input_file_name-${envname}
if [ ! -z "$input_file_ext" ]; then
    encrypted_data_outfile=${encrypted_data_outfile}.${input_file_ext}
fi

# remove existing encrypted_data_outfile file
echo " "
log_msg "Removing existing file: $encrypted_data_outfile"
rm -f $encrypted_data_outfile

# get applications private key
tmp_filename_prefix=${appname}encrypttmp
privkey_file=/tmp/${tmp_filename_prefix}_privkey.pem
read_enterprise_secret_value ${envname} privkey_file_base64_data
echo "$privkey_file_base64_data"|openssl base64 -d -A > $privkey_file

# Encrypt the input data file using private key
echo " "
log_msg "Encrypting data file to output file: $encrypted_data_outfile"
openssl enc -base64 -A -aes-256-cbc -salt -in $file_to_encrypt -out $encrypted_data_outfile -pass file:$privkey_file

echo " "
echo "=========================================================================================="
log_msg "Output file (Base64): $encrypted_data_outfile"
echo "=========================================================================================="
echo " "
cat $encrypted_data_outfile

echo " "

cd $currDir
