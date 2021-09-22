cd /jepsen/mongodb
lein run test-all -w list-append -n n1 -n n2 -n n3 -n n4 -n n5 -n n6 -n n7 -n n8 -n n9 -r 1000 --concurrency 3n --time-limit 120 --max-writes-per-key 128 --read-concern majority --write-concern majority --txn-read-concern snapshot --txn-write-concern majority --nemesis-interval 1 --nemesis partition --test-count 30
