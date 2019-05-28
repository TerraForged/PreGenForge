# PreGenForge
Forge chunk pre-generator

## Commands

#### Create
Setup and start a new pre-generator with the given radius of chunks around the center block coords
```
/pregen create <centerX> <centerZ> <chunkRadius>
```

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
Control the number of chunk requests that are made before waiting for some to complete generating
```]
/pregen limit <1-100>
```