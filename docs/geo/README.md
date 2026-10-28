# Locations

`Location` (`src/commonMain/kotlin/com/eskotech/luach/geo/Location.kt`) is the place for which times are computed: a latitude, a longitude, a `kotlinx.datetime.TimeZone`, an elevation in metres (default 0) and an optional name. It is a data class, so two locations with the same fields are equal and `copy` changes one field. Two halachic flags travel with it, `inIsrael` and `mukefChoma` (both default `false`): `Zmanim.hebrewDay` and `Zmanim.isAsurBemelacha` read them, so a place is described once and every day computed for it knows whether it keeps one day of Yom Tov and whether Purim is on the 15th.

## Validation

The constructor (and therefore `copy`) accepts a latitude in `-90..90`, a longitude in `-180..180` and a finite elevation of at least 0, and throws `IllegalArgumentException` for anything else; `NaN` fails every range check.

## Coordinates from degrees, minutes and seconds

`Location.coordinate(degrees, minutes, seconds, direction)` returns a decimal degree value: `degrees + minutes / 60 + seconds / 3600`, negated for `S` and `W` (the direction letter is case-insensitive). Degrees are 0 or more, minutes are in `0..59`, seconds in `0 ≤ s < 60`, and the direction letter is one of the four; anything else throws. The result is validated by `Location` when it is passed in.

## Distances and bearings

| Call | Result | Method |
| --- | --- | --- |
| `a.geodesicTo(b)` | `Geodesic(distance, initialBearing, finalBearing)` | Vincenty's inverse solution on the WGS84 ellipsoid (equatorial radius 6378137 m, polar radius 6356752.3142 m, flattening 1/298.257223563), iterated to 1e-12 with at most 20 iterations. Distance in metres; bearings in degrees clockwise from north in `-180..180`. |
| `a.rhumbLineTo(b)` | `RhumbLine(distance, bearing)` | The constant-bearing path on a sphere of radius 6378137 m. |

Two coincident points give a geodesic distance of `0.0` with `NaN` bearings (a bearing is undefined). For nearly antipodal points, where the Vincenty iteration fails to converge, every field is `NaN`. The time zone and elevation play no part in either calculation.

`LocationTest` checks the values against hand-computed references (equator and meridian spans, New York to London, Lakewood to Jerusalem, Sydney to Los Angeles) and the validation rules; `LocationSnapshotTest` fingerprints every pair of twenty places and some poles and antipodes.
