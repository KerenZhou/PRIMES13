#!/usr/bin/env python3

import subprocess, multiprocessing, platform
from matplotlib.backends.backend_pdf import PdfPages
pp = PdfPages('res.pdf')
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec
import sys
import argparse

machines = {
	8 : { "name" : platform.node(), "step" : 1 },
	48 : { "name" : "tom", "step" : 6 },
	80 : { "name" : "ben",  "step" : 10 }
}

time = 0.25
def main():
	cores = multiprocessing.cpu_count()
	thread_counts = list(range(0, cores + 1, machines[cores]["step"]))[1:]
	if thread_counts[0] != 1: thread_counts = [1] + thread_counts
	
	cmd = ["java", "-Xmx16G", "-Xms16G", "-cp", "PRIMES13.jar"]
	results = runprog(cmd, "primes13.Tester", time, cores, machines[cores]["step"])
	f = open("res.txt", "w")
	
	print("SkipList", file=f)
	for r in results:
		print(r, file=f)

	x = [1, 2, 3, 4, 5]
	y = thread_counts
	fig = plt.figure()
	p = fig.add_subplot(111, projection="3d")
	p.set_title("Rates")
	p.set_xlabel("threads")
	p.set_ylabel("ops/ms")
	p.set_zlabel("ops/ms")
	p.plot([1, 2, 3, 4, 5], thread_counts, [r[i + 1] for r in results])
	pp.savefig()
	pp.close()

def runprog(cmd, main_class, runtime, maxThreads, threadStep):
	print("Starting test with %d thread(s)" % (threads,))
	ret = []
	res = subprocess.check_output(cmd + ["-agentlib:hprof=cpu=samples", main_class, str(runtime), str(maxThreads), str(threadStep)])
	for subRes in res.split("\n")
		ret.append(tuple([threads] + [float(n) for n in subRes.decode('ascii').strip().split(',')]))
	return ret

if __name__ == '__main__':
	main()