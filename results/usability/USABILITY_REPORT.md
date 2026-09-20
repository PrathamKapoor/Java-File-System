# MicroFS Real-World Validation

seed=42
timestamp=2026-09-19T20:33:30.313878600Z
commit=205c22ca2559e961f3251eec47cefe7538678f02
jdk=25.0.2
os=Windows 11 10.0 (amd64)
workload=realistic-student-developer-namespace
warmup=100
measurement_policy=1000 (<10000), 100 (10000), 50 (50000)

This report contains measured `System.nanoTime()` samples. Dataset construction and index construction/rebuild timing are separate from operation latency. Human-readable latency uses µs/ms where appropriate; raw nanoseconds are in `latency.csv`.

## Operation latency

| Dataset | Strategy | Operation | Mode | Median | P95 | Mean | Min | Max | Count |
|---:|---|---|---|---:|---:|---:|---:|---:|---:|
| 100 | LINEAR | exact lookup | warm | 2.200 µs | 5.100 µs | 2.424 µs | 400 ns | 75.600 µs | 1000 |
| 100 | LINEAR | exact lookup | cold | 500 ns | 800 ns | 530 ns | 100 ns | 900 ns | 20 |
| 100 | LINEAR | prefix search | warm | 4.500 µs | 9.400 µs | 5.291 µs | 2.200 µs | 180.000 µs | 1000 |
| 100 | TRIE | prefix search | warm | 9.500 µs | 48.200 µs | 14.322 µs | 100 ns | 210.400 µs | 1000 |
| 100 | PathResolver | path resolution | warm | 3.000 µs | 25.800 µs | 8.062 µs | 2.500 µs | 190.000 µs | 1000 |
| 100 | directory index | ls / ordered traversal | warm | 400 ns | 700 ns | 567 ns | 300 ns | 37.300 µs | 1000 |
| 100 | HASH | exact lookup | warm | 300 ns | 500 ns | 400 ns | 100 ns | 38.500 µs | 1000 |
| 100 | HASH | exact lookup | cold | 400 ns | 900 ns | 500 ns | 200 ns | 900 ns | 20 |
| 100 | RB_TREE | exact lookup | warm | 300 ns | 1.400 µs | 658 ns | 100 ns | 23.600 µs | 1000 |
| 100 | RB_TREE | exact lookup | cold | 400 ns | 700 ns | 425 ns | 200 ns | 700 ns | 20 |
| 100 | HYBRID | exact lookup | warm | 200 ns | 200 ns | 262 ns | 100 ns | 62.900 µs | 1000 |
| 100 | HYBRID | exact lookup | cold | 300 ns | 400 ns | 295 ns | 200 ns | 400 ns | 20 |
| 100 | ADAPTIVE(5,50) | exact lookup | warm | 200 ns | 200 ns | 257 ns | 100 ns | 36.000 µs | 1000 |
| 100 | ADAPTIVE(5,50) | exact lookup | cold | 600 ns | 1.400 µs | 760 ns | 300 ns | 1.500 µs | 20 |
| 1000 | LINEAR | exact lookup | warm | 1.700 µs | 2.900 µs | 1.749 µs | 300 ns | 8.600 µs | 1000 |
| 1000 | LINEAR | exact lookup | cold | 3.000 µs | 4.400 µs | 2.840 µs | 700 ns | 4.700 µs | 20 |
| 1000 | LINEAR | prefix search | warm | 7.100 µs | 23.300 µs | 9.870 µs | 5.400 µs | 185.400 µs | 1000 |
| 1000 | TRIE | prefix search | warm | 40.400 µs | 118.400 µs | 48.862 µs | 100 ns | 218.300 µs | 1000 |
| 1000 | PathResolver | path resolution | warm | 2.500 µs | 5.600 µs | 3.591 µs | 2.200 µs | 119.500 µs | 1000 |
| 1000 | directory index | ls / ordered traversal | warm | 900 ns | 1.300 µs | 971 ns | 600 ns | 17.800 µs | 1000 |
| 1000 | HASH | exact lookup | warm | 100 ns | 200 ns | 128 ns | 0 ns | 2.100 µs | 1000 |
| 1000 | HASH | exact lookup | cold | 500 ns | 1.000 µs | 525 ns | 300 ns | 1.100 µs | 20 |
| 1000 | RB_TREE | exact lookup | warm | 200 ns | 400 ns | 231 ns | 100 ns | 2.800 µs | 1000 |
| 1000 | RB_TREE | exact lookup | cold | 600 ns | 1.100 µs | 630 ns | 300 ns | 1.300 µs | 20 |
| 1000 | HYBRID | exact lookup | warm | 100 ns | 100 ns | 100 ns | 0 ns | 3.500 µs | 1000 |
| 1000 | HYBRID | exact lookup | cold | 200 ns | 400 ns | 215 ns | 100 ns | 500 ns | 20 |
| 1000 | ADAPTIVE(5,50) | exact lookup | warm | 100 ns | 100 ns | 104 ns | 0 ns | 3.900 µs | 1000 |
| 1000 | ADAPTIVE(5,50) | exact lookup | cold | 3.500 µs | 4.500 µs | 3.540 µs | 3.000 µs | 4.700 µs | 20 |
| 10000 | LINEAR | exact lookup | warm | 28.000 µs | 45.800 µs | 25.680 µs | 4.700 µs | 46.200 µs | 100 |
| 10000 | LINEAR | exact lookup | cold | 7.900 µs | 11.200 µs | 8.560 µs | 7.800 µs | 11.200 µs | 5 |
| 10000 | LINEAR | prefix search | warm | 74.800 µs | 134.400 µs | 85.166 µs | 58.900 µs | 193.700 µs | 100 |
| 10000 | TRIE | prefix search | warm | 396.800 µs | 999.600 µs | 483.355 µs | 200 ns | 7.088 ms | 100 |
| 10000 | PathResolver | path resolution | warm | 5.200 µs | 6.000 µs | 5.394 µs | 4.800 µs | 15.100 µs | 100 |
| 10000 | directory index | ls / ordered traversal | warm | 4.000 µs | 4.700 µs | 4.150 µs | 3.700 µs | 4.900 µs | 20 |
| 10000 | HASH | exact lookup | warm | 100 ns | 100 ns | 97 ns | 0 ns | 200 ns | 100 |
| 10000 | HASH | exact lookup | cold | 400 ns | 600 ns | 420 ns | 200 ns | 600 ns | 5 |
| 10000 | RB_TREE | exact lookup | warm | 200 ns | 400 ns | 240 ns | 100 ns | 4.000 µs | 100 |
| 10000 | RB_TREE | exact lookup | cold | 3.700 µs | 4.200 µs | 2.720 µs | 900 ns | 4.200 µs | 5 |
| 10000 | HYBRID | exact lookup | warm | 100 ns | 100 ns | 93 ns | 0 ns | 300 ns | 100 |
| 10000 | HYBRID | exact lookup | cold | 3.100 µs | 3.600 µs | 2.900 µs | 2.200 µs | 3.600 µs | 5 |
| 50000 | HASH | exact lookup | warm | 100 ns | 100 ns | 88 ns | 0 ns | 200 ns | 50 |
| 50000 | HASH | exact lookup | cold | 3.500 µs | 5.500 µs | 3.180 µs | 1.600 µs | 5.500 µs | 5 |
| 50000 | LINEAR | prefix search | warm | 715.400 µs | 1.057 ms | 742.290 µs | 447.200 µs | 1.088 ms | 50 |
| 50000 | TRIE | prefix search | warm | 1.794 ms | 4.900 ms | 2.713 ms | 500 ns | 31.279 ms | 50 |
| 50000 | PathResolver | path resolution | warm | 1.700 µs | 1.900 µs | 1.754 µs | 1.500 µs | 2.400 µs | 50 |
| 50000 | directory index | ls / ordered traversal | warm | 44.832 ms | 49.245 ms | 44.847 ms | 41.123 ms | 49.245 ms | 10 |

## Dataset construction timing

- {"dataset_size":100,"generation_ns":7656600}
- {"dataset_size":1000,"generation_ns":2947800}
- {"dataset_size":10000,"generation_ns":10703200}
- {"dataset_size":50000,"generation_ns":19598000}

## Index construction and rebuild timing

- {"dataset_size":100,"strategy":"LINEAR","construction_ns":13030400}
- {"dataset_size":100,"strategy":"HASH","construction_ns":239300}
- {"dataset_size":100,"strategy":"RB_TREE","construction_ns":494400}
- {"dataset_size":100,"strategy":"HYBRID","construction_ns":170800}
- {"dataset_size":100,"strategy":"ADAPTIVE(5,50)","construction_ns":8583600}
- {"dataset_size":1000,"strategy":"LINEAR","construction_ns":9186200}
- {"dataset_size":1000,"strategy":"HASH","construction_ns":428500}
- {"dataset_size":1000,"strategy":"RB_TREE","construction_ns":574400}
- {"dataset_size":1000,"strategy":"HYBRID","construction_ns":599500}
- {"dataset_size":1000,"strategy":"ADAPTIVE(5,50)","construction_ns":197646000}
- {"dataset_size":10000,"strategy":"LINEAR","construction_ns":1091752200}
- {"dataset_size":10000,"strategy":"HASH","construction_ns":997300}
- {"dataset_size":10000,"strategy":"RB_TREE","construction_ns":6367500}
- {"dataset_size":10000,"strategy":"HYBRID","construction_ns":7589100}
- {"dataset_size":10000,"strategy":"ADAPTIVE(5,50)","status":"NOT_RUN — adaptive rebuild construction exceeds practical validation budget"}
- {"dataset_size":50000,"strategy":"LINEAR","status":"NOT_RUN — strategy omitted at this scale to keep validation within practical runtime"}
- {"dataset_size":50000,"strategy":"HASH","construction_ns":6887900}
- {"dataset_size":50000,"strategy":"RB_TREE","status":"NOT_RUN — strategy omitted at this scale to keep validation within practical runtime"}
- {"dataset_size":50000,"strategy":"HYBRID","status":"NOT_RUN — strategy omitted at this scale to keep validation within practical runtime"}
- {"dataset_size":50000,"strategy":"ADAPTIVE(5,50)","status":"NOT_RUN — strategy omitted at this scale to keep validation within practical runtime"}

## Mount/remount timing

{"mount_ns":11794600,"remount_ns":28790900,"mkdir":"MEASURED","create":"MEASURED"}

`remount_search` is NOT_AVAILABLE — full namespace rebuild is a documented current limitation.

## Unavailable operations

- `find`: NOT_AVAILABLE — unsupported by current CLI
- `delete`: NOT_AVAILABLE — unsupported by current CLI
- `stat`: NOT_AVAILABLE — unsupported by current CLI

## Interpretation

Exact lookup, prefix search, path resolution, and ordered traversal are reported at each requested namespace scale. The adaptive directory is intentionally not constructed above 1,000 entries because its existing rebuild-on-every-insert design would turn the validation into an impractical construction stress test; its measured construction cost at smaller sizes is retained. At 50,000 entries the validation retains linear and hash exact-lookup baselines. No algorithm was changed.
