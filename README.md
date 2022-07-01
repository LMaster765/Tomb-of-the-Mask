# **Tomb of the Mask**

## **Changelog**

### Version 1.1

#### TODO

- Implement static tilesheets for player
- Finish new render system
  - Color layer (+ rising water)
  - Invert tilesheets so they hide color layer instead of adding color

#### 07/01/22

- New render method that is SIGNIFICANTLY faster
  - Image scaling is done in the `Cell` and `Entity` classes per tile/entity
  - Scaled tiles are drawn to the full-size layer
  - Now, only tiles that need to be updated are scaled each frame instead of the entire image
  - Alltogether, the game runs at 3-4 ms per frame instead of 13-18 ms

#### 06/30/22

- Created the `TimeTracker` class for determining the cause of lag
- Changed image scaling method to increase speed
- Image scaling still takes the majority of each tick (12 of 15 ms)

#### 06/29/22

- New `Frame` class handles the frame layers and rendering
  - This unfortunately added a ton of lag (idk why) that needs to be reduced

#### 06/28/22

- `Entity` images also use static tilesheets now

#### 06/??/22

- `Cell` images are now stored in static tilesheets instead of individual files

### **Version 1.0** (AP Comp Sci Final Project Version)

- It works! (intro, menu, some levels, in-level graphics + animations)
