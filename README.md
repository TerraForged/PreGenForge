# PreGenForge
Forge chunk pre-generator

## Commands

#### Create
Setup a new pre-generator with the given radius of chunks around the center block coords
```
/pregen create <centerX> <centerZ> <chunkRadius>
```

Notes:
- generation comences automatically after creating the pregenerator
- the pregenerator will persist through restarts until the task is complete or cancelled

#### Start
Start a paused pre-generator
```
/pregen start
```

#### Pause
Pause a running pre-generator
```
/pregen pause
```

#### Cancel
Stop and delete a pre-generator
```
/pregen cancel
```

#### Limit
Control the number of chunks to queue per tick
```]
/pregen limit <1-100>
```

Notes:
- lower values will save cpu/ram usage but increase the overall task time
- higher values will shorten the task time but increase cpu/ram usage
- **if cpu/ram usage gets too high, the entire server may grind to a snails pace negating any of the benefits of the higher limit value**
