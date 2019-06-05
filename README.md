# PreGenForge
Forge chunk pre-generator

## Commands

#### Start
Setup a new pre-generator with the given radius of chunks centred around the given block coords
```
/pregen start <centreX> <centreZ> <radius>
```

Arguments:
- centreX/centreZ - the centre block position to generate chunks around
- radius - the radius in chunks to generate

```
/pregen start <centreX> <centreZ> <fromRadius> <toRadius>
```

Arguments:
- centreX/centreZ - the centre block position to generate chunks around
- fromRadius - the radius in chunks at which the generator should start
- toRadius - the radius in chunks at which the generator should finish


#### Pause
Pause a running pre-generator
```
/pregen pause
```


#### Resume
Resume a paused pre-generator
```
/pregen resume
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