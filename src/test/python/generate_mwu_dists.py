#!/usr/bin/env python

# The MWU test values were pulled from the Python moztelemetry project, which
# tests against scipy.
#
# Below is sample code to create the input distributions and resulting values
# that can be copied into the MWU tests.

import numpy
import scipy.stats

from moztelemetry import stats


def scala_print(d):
    s = ['%s -> %s' % (k, v) for k, v in d.items()]
    print 'Map(' + ', '.join(s) + ')'


def l2d(values):
    # Convert a list of values to a histogram representation.
    d = {}
    for v in values:
        d[v] = d.get(v, 0) + 1
    return d


def gen_normal_distribution():
    print "Generating 2 normal distributions..."
    norm1 = l2d([int(round(x))
                 for x in list(numpy.random.normal(5, 3.25, 1000))])
    norm2 = l2d([int(round(x))
                 for x in list(numpy.random.normal(6, 2.5, 1000))])
    print "Normalized distribution 1:"
    scala_print(norm1)
    print "Normalized distribution 2:"
    scala_print(norm2)
    print "MWU result with useContinuity == true"
    print stats.mann_whitney_u(norm1, norm2)
    print "MWU result with useContinuity == false"
    print stats.mann_whitney_u(norm1, norm2, False)


def gen_uniform_distribution():
    print "Generating 2 uniform distributions..."
    uni1 = l2d(numpy.random.randint(1, 50, 1000))
    uni2 = l2d(numpy.random.randint(10, 60, 900))
    print "Uniform distribution 1:"
    scala_print(uni1)
    print "Uniform distribution 2:"
    scala_print(uni2)
    print "MWU result with useContinuity == true"
    print stats.mann_whitney_u(uni1, uni2)
    print "MWU result with useContinuity == false"
    print stats.mann_whitney_u(uni1, uni2, False)


def gen_skewed_distribution():
    print "Generating 2 skewed distributions..."
    skew1 = l2d([int(round(x))
                 for x in list(scipy.stats.skewnorm.rvs(10, size=1000))])
    skew2 = l2d([int(round(x))
                 for x in list(scipy.stats.skewnorm.rvs(5, size=900))])
    print "Skewed distribution 1:"
    scala_print(skew1)
    print "Skewed distribution 2:"
    scala_print(skew2)
    print "MWU result with useContinuity == true"
    print stats.mann_whitney_u(skew1, skew2)
    print "MWU result with useContinuity == false"
    print stats.mann_whitney_u(skew1, skew2, False)


if __name__ == "__main__":
    gen_normal_distribution()
    gen_uniform_distribution()
    gen_skewed_distribution()
