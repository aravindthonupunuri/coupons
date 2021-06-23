#!/usr/bin/env bash

#app details
currDir=`pwd`
scriptDir=`dirname $0`
if [[ "$scriptDir" =~ ^\..* ]]; then
   scriptDir="$currDir/$scriptDir"
fi
appname=backpackregistrycouponsconsumer
gitorg=Registry-Modernization
gitrepo=backpack-registry-coupons
secretorg=registries-modernization
resources_location=$scriptDir/../src/main/resources
secret_resources_location=$resources_location/secrets
service_resources_location=${scriptDir}/../../backpack-registry-coupons-service/src/main/resources
data_folder=${scriptDir}/../../data

DEV_ENVIRONMENT="dev"
STAGE_ENVIRONMENT="stage"
PROD_ENVIRONMENT="prod"
SUPPORTED_ENVIRONMENTS="$DEV_ENVIRONMENT $STAGE_ENVIRONMENT $PROD_ENVIRONMENT"

# validate appinfo contains correct git repo for this project
if [[ "$scriptDir" != *\/"$gitrepo"\/* ]]; then
    echo "[$scriptname] appinfo.sh gitrepo \"$gitrepo\" doesn't belong to project for script $scriptDir/$scriptname.sh"
    exit 1
fi

# make sure VAULT_ADDR environment variable is defined
if [[ -z "${VAULT_ADDR}" ]]; then
    if [[ -z "${CICD_MODE}" ]]; then
        echo "[$scriptname] appinfo.sh - Missing Environment Variable VAULT_ADDR"
        exit 1
    fi
fi
