#!/bin/bash
#
# Simple Archive Format
#


#set -C

if test "_$1_" = _c_
then
   shift
   #find "$@" -maxdepth 0 -printf '%s %p\n'
   find "$@" -type f -exec stat -f "%z %N" {} \;
   echo -
   cat "$@"
elif test "_$1_" = _x_
then
   shift
   count=0

   while read size name && test "_${size##[^0-9]}_" = "_${size}_"
   do
      sizes[count]=$((size))
      names[count]=${name}
      count=$((count+1))
   done

   if test "_${size}_" != "_-_"
   then
      echo "Bad dir"
      exit
   fi

   i=0
   while let 'i<count'
   do
      echo ${names[i]} ${sizes[i]}
      if test "_${names[i]##*/}_" != "_${names[i]}_"
      then
         install -d "${names[i]%/*}"
      fi
      dd bs=${sizes[i]} count=1 >"${names[i]}" 2>/dev/null
      i=$((i+1))
   done

else
   echo "To create an archive use: saf.sf c files..."
   echo "To extract an archive use: saf.sf x"
fi