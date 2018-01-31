# Xposed-ScreenOffAnimation
Xposed: Custom Screen Off Animations for all ROMs
Fork from https://github.com/zst123/Xposed-ScreenOffAnimation

changes:
ported to Android Studio
added CRT screen On animation

TODO:
-fix screen on animation delay in an adaptive way

  ideas:
  
  firstly lower the delay
  
  start the animation at the half of the delay, if in the other half the screen is still black, restart
  
  find another hooking point 
  
  add a black screen and when the animation starts it removes it, so no content will be shown
  
-add samsung screenoff animation

-sign the app (sry I haven't done it already)

-remove shared preferences or workaround so for oreo can work
