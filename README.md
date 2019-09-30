# PreGenForge
Forge chunk pregenerator

##### Notes
 - it's **not** a good idea to run pregeneration on a live/production server
 - it's a good idea to set the server's `max-tick-time` property to `-1` when running pregeneration

## Commands

#### Start
Start a new pregenerator centered on `(centerX,centreZ)` with the given chunk `radius`
```
/pregen start <centreX> <centreZ> <radius>
```

Arguments:
- centreX/centreZ - the centre block position to generate chunks around
- radius - the radius in chunks to generate

#### Expand
Starts a new pregenerator centered on `(centerX,centreZ)` expanding from the given `startRadius` outwards to the
 given `endRadius`
```
/pregen expand <centreX> <centreZ> <startRadius> <endRadius>
```

Arguments:
- centreX/centreZ - the centre block position to generate chunks around
- startRadius - the radius in chunks at which the generator should start
- endRadius - the radius in chunks at which the generator should finish


#### Pause
Pause a running pregenerator
```
/pregen pause
```


#### Resume
Resume a paused pregenerator
```
/pregen resume
```


#### Cancel
Stop and delete a pregenerator
```
/pregen cancel
```

## Utilities

#### Server Property Fixer
There's a weird bug with forge?/vanilla where certain server settings (in the `server.properties` file) get reset to
 defaults each time the server starts. This mod includes a way to work around this issue. Simply create a file called
 `overrides.properties` in your server root directory and add any standard server properties you like in there.