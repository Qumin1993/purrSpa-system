# Original Purr Spa cat illustrations

The two Android VectorDrawable assets were created specifically for the Purr Spa System project:

- `app/src/main/res/drawable/purr_cat_ginger.xml` — warm ginger cat for sidebar branding.
- `app/src/main/res/drawable/purr_cat_charcoal.xml` — charcoal cat for the dashboard.

Both are original hand-authored vector shapes (no third-party image, screenshot trace or external font). They are editable XML resources and scale without raster blur.

Usage: `Image(painterResource(R.drawable.purr_cat_ginger), contentDescription = "...")`.

Design status: initial illustration set. Confirm final art direction, accessibility contrast and tablet rendering during Android device review.
