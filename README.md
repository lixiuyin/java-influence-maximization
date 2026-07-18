# Java Influence Maximization on Signed Networks

[简体中文](README.zh-CN.md)

A Java course project for selecting 10 seed users that maximize expected positive influence in a directed, signed social network. The implementation combines a signed independent-cascade diffusion model, Monte Carlo estimation, a greedy seed-selection strategy, and multithreaded candidate evaluation.

## Problem

Given a directed social network, a probabilistic diffusion model, and a seed budget `k = 10`, select a seed set that maximizes the expected number of positively activated nodes.

Each edge is represented as:

```text
source_node target_node sign activation_probability
```

- `sign` is `1` for a positive relation and `-1` for a negative relation.
- A newly activated node gets one chance to activate each inactive out-neighbor.
- If activation succeeds, the target state is `source_state * edge_sign`.
- The cascade stops when there are no newly activated nodes.
- Because diffusion is stochastic, expected positive influence is estimated by repeated Monte Carlo simulation.

The assignment required:

- a class-based Java implementation of the diffusion model and seed-selection workflow;
- the 10 selected seeds, the influence obtained at seed-set sizes 1 through 10, and runtime information;
- comparison against the top-10 out-degree and positive-out-degree heuristics;
- an explanation of class responsibilities, key code, and results.

## Implementation

| Class | Responsibility |
| --- | --- |
| `DiffusionModel` | Simulates signed cascades and estimates average positive influence. |
| `InitialNodesSelector` | Greedily adds the candidate with the largest estimated marginal influence. |
| `GreedyAlgorithmMain` | Loads the graph and runs the 10-seed greedy search. |
| `MethodComparison` | Evaluates five greedy solutions and the two degree-based baselines. |
| `ToolBoxAPI` | Handles data loading, adjacency-map caching, degree statistics, timing, and plotting. |

Candidate additions are evaluated concurrently with a fixed thread pool, while independent Monte Carlo cascades use parallel streams. The submitted implementation is preserved; only the original machine-specific data and cache paths were changed to repository-relative paths.

![CPU utilization during the multithreaded search](assets/cpu-utilization.png)

## Dataset

The included network contains:

| Item | Count |
| --- | ---: |
| Nodes | 10,966 |
| Directed edges | 44,356 |
| Positive edges | 32,822 |
| Negative edges | 11,534 |

- [`data/nodes.txt`](data/nodes.txt) contains one node identifier per line.
- [`data/edges.txt`](data/edges.txt) contains source, target, sign, and activation probability.
- `data/adjacency-map.ser` may be generated locally as an optional cache and is ignored by Git.

## Reported results

The final reported greedy run used 500 simulations for each influence estimate. The resulting seed set was:

```text
[184, 15, 1832, 163, 1116, 165, 1287, 1397, 1556, 1808]
```

| Seed-set size | Added node | Mean positive influence |
| ---: | ---: | ---: |
| 1 | 184 | 27 |
| 2 | 15 | 51 |
| 3 | 1832 | 73 |
| 4 | 163 | 91 |
| 5 | 1116 | 109 |
| 6 | 165 | 122 |
| 7 | 1287 | 133 |
| 8 | 1397 | 145 |
| 9 | 1556 | 156 |
| 10 | 1808 | 167 |

The selected set was re-evaluated with 100,000 Monte Carlo simulations. The reported comparison was:

| Seed-selection method | Mean positive influence |
| --- | ---: |
| Greedy selection | **167** |
| Top-10 out-degree | 149 |
| Top-10 positive out-degree | 149 |

The greedy solution therefore improved the reported mean by 18 positively activated nodes, or approximately 12.1%, over either degree-based baseline. Results can vary slightly between runs because the diffusion process is stochastic.

![Greedy seed-selection progress](assets/greedy-selection-progress.png)

![Convergence of the two degree-based baselines](assets/baseline-convergence.png)

## Build and run

Requirements: JDK 17 or newer and GNU Make.

```bash
make build
make run-greedy
make run-comparison
```

Equivalent commands:

```bash
javac -encoding UTF-8 -d build/classes src/*.java
java -cp build/classes GreedyAlgorithmMain
java -cp build/classes MethodComparison
```

Run commands from the repository root so the programs can resolve `data/nodes.txt` and `data/edges.txt`. The full greedy search is computationally expensive; the reported 500-simulation run took about 4 hours 10 minutes on an Intel Core i7-12700H.

## Repository structure

```text
.
├── assets/             # Renamed result figures used in this README
├── data/               # Node and signed-edge lists
├── src/                # Original Java implementation with portable paths
├── .github/workflows/  # Compilation check
├── Makefile
├── README.md
└── README.zh-CN.md
```

The course presentation and report PDFs are intentionally not included.
