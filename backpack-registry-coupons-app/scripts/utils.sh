#!/usr/bin/env bash

PRIVKEY="privkey" # constant value

function check_environment() {
    local env_name="$1"
    if [ -z "$env_name" ]; then
        log_usage_err "Missing deployment environment name"
    elif [[ ! "$SUPPORTED_ENVIRONMENTS" =~ .*"${env_name} ".* && ! "$SUPPORTED_ENVIRONMENTS" =~ .*"${env_name}"$ ]]; then
        log_usage_err "Invalid deployment environment name $env_name"
    fi
}

function get_enterprise_secret_token() {
  local __forced="$1"
  local  __resultvar=$2
  if [ ! -d ~/.enterprise_secret ]; then
    mkdir ~/.enterprise_secret
  fi
  log_msg "get_enterprise_secret_token: get Enterprise Secret token [forced: $__forced]"
  if [[ ! -f ~/.enterprise_secret/token || "$__forced" == true ]]; then
    user=`whoami`
    log_msg "get_enterprise_secret_token: Fetching new Enterprise Secret token for $user"
    echo -n Password:
    read -s password
    echo
    token=`curl -s ${VAULT_ADDR}/v1/auth/ldap/login/${user} -X POST --data '{"password":"'${password}'"}' | jq -r '.auth.client_token'`
    echo $token > ~/.enterprise_secret/token
  fi
  token=`cat ~/.enterprise_secret/token`
  eval $__resultvar="'$token'"
}

function read_enterprise_secret_value() {
  local __env_name="$1"
  local  __resultvar=$2

  secret_name=`build_enterprise_secret_name $__env_name`

  log_msg "read_enterprise_secret_value: fetching Enterprise Secret token value for $secret_name"
  get_enterprise_secret_token false vault_token
  secret_value=`curl -k -s -w 'httpcode:%{http_Code}\n' ${VAULT_ADDR}/v1/secret/$secret_name -H "X-Vault-Token: ${vault_token}" 2>&1`
  if [[ "$secret_value" =~ .*"httpcode:403".* ]]; then
    # get brand new token and retry
    log_msg "read_enterprise_secret_value: secret value fetch failed due to stale Enterprise Secret token...refreshing token now"
    get_enterprise_secret_token true vault_token
    secret_value=`curl -k -s ${VAULT_ADDR}/v1/secret/$secret_name -H "X-Vault-Token: ${vault_token}" 2>&1`
    secret_data=`echo $secret_value | jq -r ".data.$PRIVKEY"`
    eval $__resultvar="'$secret_data'"
  elif [[ "$secret_value" =~ .*"httpcode:404".* ]]; then
    log_msg "read_enterprise_secret_value: No secret value found in Enterprise Secrets for secret name $secret_name"
    eval $__resultvar="404"
  fi
  if [[ "$secret_value" =~ .*"httpcode:200".* ]]; then
    secret_value=${secret_value/httpcode:200/}
    secret_data=`echo $secret_value | jq -r ".data.$PRIVKEY"`
    eval $__resultvar="'$secret_data'"
  fi
}

function write_enterprise_secret_value() {
  local __env_name="$1"
  local __secret_value="$2"

  secret_name=`build_enterprise_secret_name $__env_name`

  log_msg "write_enterprise_secret_value: write Enterprise Secret token value for $secret_name"
  get_enterprise_secret_token false vault_token
  secret_value=`curl -k -s -w 'httpcode:%{http_Code}\n' -X POST ${VAULT_ADDR}/v1/secret/$secret_name -H "X-Vault-Token: ${vault_token}" -H "Content-Type: application/json" --data '{"'$PRIVKEY'":"'$__secret_value'"}' 2>&1`
  if [[ "$secret_value" =~ .*"httpcode:403".* ]]; then
    # get brand new token and retry
    log_msg "write_enterprise_secret_value: secret value write failed due to stale Enterprise Secret token...refreshing token now"
    get_enterprise_secret_token true vault_token
    secret_value=`curl -k -s -X POST ${VAULT_ADDR}/v1/secret/$secret_name -H "X-Vault-Token: ${vault_token}" -H "Content-Type: application/json" --data '{"'$PRIVKEY'":"'$__secret_value'"}' 2>&1`
  fi
  log_msg "write_enterprise_secret_value: successfully added enterprise secret $secret_name"
}

function build_enterprise_secret_name() {
  local __env_name="$1"
#  secretorg=${secretorg,,}
  echo "${secretorg}/${__env_name}/${gitrepo}/$appname"
}
