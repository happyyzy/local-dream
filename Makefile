.PHONY: check benchmark-example

check:
	bash tools/check_repo.sh

benchmark-example:
	python3 scripts/run_benchmark.py \
	  --model sdxl --resolution 1024 --steps 20 --sampler euler_a \
	  --latencies-ms 812,801,798,805,799 \
	  --notes "example" \
	  --output benchmarks/results/benchmark_results.csv
