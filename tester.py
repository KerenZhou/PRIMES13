#!/usr/bin/env python3

import subprocess, multiprocessing, platform
from matplotlib.backends.backend_pdf import PdfPages
pp = PdfPages('res.pdf')
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec

machines = {
	8 : { "name" : platform.node(), "step" : 1 },
	48 : { "name" : "tom", "step" : 2 },
	80 : { "name" : "ben",  "step" : 2 }
}

def main():
	cores = multiprocessing.cpu_count()
	thread_counts = list(range(0, cores + 1, machines[cores]["step"]))[1:]
	if thread_counts[0] != 1: thread_counts = [1] + thread_counts
	
	res_sl = []
	res_stl_sl = []
	
	cmd = ["java", "-Xmx16G", "-cp", "PRIMES13.jar"]

	# Test multi-threaded
	for tcount in thread_counts:
		sl, stl_sl = runprog(cmd, "primes13.Tester", tcount, 0.5, ["sl", "stl_sl"])
		res_sl.append(sl)
		res_stl_sl.append(stl_sl)

	# Test single-threaded
	ts = runprog(cmd, "primes13.Tester", 1, 0.5, ["stl_ts"])
	
	f = open("res.txt", "w")
	
	print("STL TreeSet", file=f)
	print(ts[0], file=f)

	print("STL SkipList", file=f)
	for r in res_stl_sl:
		print(r, file=f)

	print("Custom", file=f)
	for r in res_sl:
		print(r, file=f)

	labels = ["Lookup", "Add", "Remove"]
	for i in range(3):
		fig = plt.figure()
		p = fig.add_subplot(111)
		p.set_title(labels[i] + " Rates")
		p.set_xlabel("threads")
		p.set_ylabel("ops/ms")
		p.plot(thread_counts, [r[i + 1] for r in res_stl_sl], c='r')
		p.plot(thread_counts, [r[i + 1] for r in res_sl], c='g')
		pp.savefig()
	pp.close()

def runprog(cmd, main_class, threads, runtime, types):
	print("Starting test with %d thread(s)" % (threads,))
	ret = []
	for t in types:
		print(" -> Testing type class: %s" % (t,))
		res = subprocess.check_output(cmd + [main_class, str(runtime), str(threads), t])
		ret.append(tuple([threads] + [float(n) for n in res.decode('ascii').strip().split(',')]))
	return ret

if __name__ == '__main__':
	main()