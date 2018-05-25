#!/bin/bash
set -e

sudo -E docker stack deploy --compose-file docker-compose.yml lexpredict-tika-cluster
