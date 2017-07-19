/*

program by user1502040
score 2,075,452

https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai/114172#114172

published on 2017-03-28


C - 2,075,452

I know I'm extremely late to the party, but I saw this challenge and wanted
to have a go.

The algorithm is based on Monte-Carlo Tree Search with Thompson sampling,
and a transposition table to reduce the search space.
It takes about 12 hours on my machine. If you want to check the results,
you can find them at https://dropfile.to/pvjYDMV.

*/



/*

bugfixes applied:
- added line #include <limits.h>
- added "%len" to this line in funtion main():
    hash ^= zobrist_table[i%len][(int)solution[i]];

compile:
gcc -O2 01_user1502040.c -o 01_user1502040 -lm

run:
time nice ./01_user1502040 > steps_01_user1502040.txt

*/



#include <limits.h>

#include <assert.h>
#include <math.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

uint64_t rand_state;

uint64_t rand_u64(void) {
    return (rand_state = rand_state * 6364136223846793005ULL + 1442695040888963407ULL);
}

uint64_t better_rand_u64(void) {
    uint64_t r = rand_u64();
    r ^= ((r >> 32) >> (r >> 60));
    return r + 1442695040888963407ULL;
}

uint32_t rand_u32(void) {return rand_u64() >> 32;}

float normal(float mu, float sigma) {
    uint64_t t = 0;
    for (int i = 0; i < 6; i++) {
        uint64_t r = rand_u64();
        uint32_t a = r;
        uint32_t b = r >> 32;
        t += a + b;
    }
    return ((float)t / (float)UINT32_MAX - 6) * sigma + mu;
}

typedef struct {
    uint8_t x;
    uint8_t y;
} Position;

#define ncolors 6
#define len 19
#define cells (len * len)
#define max_steps (len * (ncolors - 1))
#define center_x 9
#define center_y 9
#define center ((Position){center_x, center_y})

uint64_t zobrist_table[len][len];

void init_zobrist() {
    for (int y = 0; y < len; y++) {
        for (int x = 0; x < len; x++) {
            zobrist_table[y][x] = better_rand_u64();
        }
    }
}

typedef struct {
    uint64_t hash;
    uint8_t grid[len][len];
    bool interior[len][len];
    int boundary_size;
    Position boundary[cells];
} Grid;


void transition(Grid* grid, uint8_t color, int* surrounding_counts) {
    int i = 0;
    while (i < grid->boundary_size) {
        Position p = grid->boundary[i];
        uint8_t x = p.x;
        uint8_t y = p.y;
        bool still_boundary = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (!(dx == 0 || dy == 0)) {
                    continue;
                }
                int8_t x1 = x + dx;
                if (!(0 <= x1 && x1 < len)) {
                    continue;
                }
                int8_t y1 = y + dy;
                if (!(0 <= y1 && y1 < len)) {
                    continue;
                }
                if (grid->interior[y1][x1]) {
                    continue;
                }
                uint8_t color1 = grid->grid[y1][x1];
                if (color1 == color) {
                    grid->boundary[grid->boundary_size++] = (Position){x1, y1};
                    grid->interior[y1][x1] = true;
                    grid->hash ^= zobrist_table[y1][x1];
                } else {
                    surrounding_counts[color1]++;
                    still_boundary = true;
                }
            }
        }
        if (still_boundary) {
            i += 1;
        } else {
            grid->boundary[i] = grid->boundary[--grid->boundary_size]; 
        }
    }
}

void reset_grid(Grid* grid, int* surrounding_counts) {
    grid->hash = 0;
    memset(surrounding_counts, 0, ncolors * sizeof(int)); 
    memset(&grid->interior, 0, sizeof(grid->interior));
    grid->interior[center_y][center_x] = true;
    grid->boundary_size = 0;
    grid->boundary[grid->boundary_size++] = center; 
    transition(grid, grid->grid[center_y][center_x], surrounding_counts);
}

bool load_grid(FILE* fp, Grid* grid) {
    memset(grid, 0, sizeof(*grid));
    char buf[19 + 2];
    size_t row = 0;
    while ((fgets(buf, sizeof(buf), fp)) && row < 19) {
        if (strlen(buf) != 20) {
            break;
        }
        for (int i = 0; i < 19; i++) {
            if (!('1' <= buf[i] && buf[i] <= '6')) {
                return false;
            }
            grid->grid[row][i] = buf[i] - '1';
        }
        row++;
    }
    return row == 19;
}

typedef struct Node Node;

struct Node {
    uint64_t hash;
    float visit_counts[ncolors];
    float mean_cost[ncolors];
    float sse[ncolors];
};

#define iters 15000
#define pool_size 32768
#define pool_nodes (pool_size + 100)
#define pool_mask (pool_size - 1)

Node pool[pool_nodes];

void init_node(Node* node, uint64_t hash, int* surrounding_counts) {
    node->hash = hash;
    for (int i = 0; i < ncolors; i++) {
        if (surrounding_counts[i]) {
            node->visit_counts[i] = 1;
            node->mean_cost[i] = 20;
            node->sse[i] = 400;
        }
    }
}

Node* lookup_node(uint64_t hash) {
    size_t index = hash & pool_mask;
    for (int i = index;; i++) {
        uint64_t h = pool[i].hash;
        if (h == hash || !h) {
            return pool + i;
        }
    }
}

int rollout(Grid* grid, int* surrounding_counts, char* solution) {
    for (int i = 0;; i++) {
        int nonzero = 0;
        uint8_t colors[6];
        for (int i = 0; i < ncolors; i++) {
            if (surrounding_counts[i]) {
                colors[nonzero++] = i;
            }
        }
        if (!nonzero) {
            return i;
        }
        uint8_t color = colors[rand_u32() % nonzero]; 
        *(solution++) = color;
        assert(grid->boundary_size);
        memset(surrounding_counts, 0, 6 * sizeof(int));
        transition(grid, color, surrounding_counts);
    }
}

int simulate(Node* node, Grid* grid, int depth, char* solution) {
    float best_cost = INFINITY;
    uint8_t best_color = 255;
    for (int color = 0; color < ncolors; color++) {
        float n = node->visit_counts[color];
        if (node->visit_counts[color] == 0) {
            continue;
        }
        float sigma = sqrt(node->sse[color] / (n * n));
        float cost = node->mean_cost[color];
        cost = normal(cost, sigma);
        if (cost < best_cost) {
            best_color = color;
            best_cost = cost;
        }
    }
    if (best_color == 255) {
        return 0;
    }
    *solution++ = best_color;
    int score;
    int surrounding_counts[ncolors] = {0};
    transition(grid, best_color, surrounding_counts);
    Node* child = lookup_node(grid->hash);
    if (!child->hash) {
        init_node(child, grid->hash, surrounding_counts);
        score = rollout(grid, surrounding_counts, solution);
    } else {
        score = simulate(child, grid, depth + 1, solution);
    }
    score++;
    float n1 = ++node->visit_counts[best_color];
    float u0 = node->mean_cost[best_color];
    float u1 = node->mean_cost[best_color] = u0 + (score - u0) / n1;
    node->sse[best_color] += (score - u0) * (score - u1);
    return score;
}

int main(void) {
    FILE* fp;
    if (!(fp = fopen("floodtest", "r"))) {
        return 1;
    }
    Grid grid;
    init_zobrist();
    while (load_grid(fp, &grid)) {

        memset(pool, 0, sizeof(pool));
        int surrounding_counts[ncolors] = {0};

        reset_grid(&grid, surrounding_counts);
        Node root = {0};

        init_node(&root, grid.hash, surrounding_counts);

        char solution[max_steps] = {0};
        char best_solution[max_steps] = {0};

        int min_score = INT_MAX;

        uint64_t prev_hash = 0;
        uint64_t hash = 0;
        int same_count = 0;

        for (int iter = 0; iter < iters; iter++) {
            reset_grid(&grid, surrounding_counts);
            int score = simulate(&root, &grid, 1, solution);
            if (score < min_score) {
                min_score = score;
                memcpy(best_solution, solution, score);
            }
            hash = 0;
            for (int i = 0; i < score; i++) {
                hash ^= zobrist_table[i%len][(int)solution[i]];
            }
            if (hash == prev_hash) {
                same_count++;
                if (same_count >= 10) {
                    break;
                }
            } else {
                same_count = 0;
                prev_hash = hash;
            }
        }
        int i;
        for (i = 0; i < min_score; i++) {
            best_solution[i] += '1';
        }
        best_solution[i++] = '\n';
        best_solution[i++] = '\0';
        printf(best_solution);
        fflush(stdout);
    }
    return 0;
}
