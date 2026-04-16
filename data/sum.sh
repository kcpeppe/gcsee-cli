#!/usr/bin/env bash

sum=0

while read -r line; do
  for num in $line; do
    if [[ "$num" =~ ^-?[0-9]+([.][0-9]+)?$ ]]; then
      sum=$(echo "$sum + $num" | bc)
    else
      echo "Warning: Skipping non-numeric value '$num'" >&2
    fi
  done
done

echo "Sum: $sum"
