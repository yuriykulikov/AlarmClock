# Ringtone picker for application-wide ringtone default

## Summary

Application settings has a preference called "default ringtone", which replaces system-wide default
ringtone.
This RFC proposes a ringtone picker that can be used to select a ringtone for the application.

## Scope

<!--
What is the scope of this RFC?
What are the goals you are aiming to achieve and which goals are explicitly out of scope?
-->

## Motivation

<!--
Why are we doing this?
-->

Many phones do not have a default ringtone preference in settings.

## Description

The ringtone picker in settings allows picking a ringtone which will be a an application default.
Selecting the system default is possible, this will be indicated in the ringtone name and changes to
the system-wide ringtone will be reflected in the application.

The ringtone picker in individual alarms will behave differently. Use can select a ringtone from a
list of ringtones.
The list will contain a "default" entry, which will be the application default.

## Reference-level explanation

The tricky parts is to distinguish two use cases: the user selects a default ringtone for an alarm
or the user selects a ringtone, which coincidentally happens to be the application default. To
distinguish between these, we use a nice hack:

When configuring the picker:

```kotlin
defaultRingtone
    .ringtoneManagerUri()
    ?.buildUpon()
    ?.appendQueryParameter("default", "true")
    ?.build()
```

When reading the result:

```kotlin
val alarmtone: Alarmtone =
    when {
        uriString == null -> Alarmtone.Silent
        // this will only be true for pickers shown in alarms, not in the settings
        uriString.contains("default=true") -> Alarmtone.Default
        uriString == "silent" -> Alarmtone.Silent
        uriString == "default" -> Alarmtone.SystemDefault
        uriString == RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString() ->
            Alarmtone.SystemDefault
        else -> Alarmtone.Sound(uriString)
    }
```

## Drawbacks, risks and assumptions

<!--
What are the drawbacks of doing this?
What risks are related to this RFC?
What assumptions are we making?
What happens if an assumption is proven to be false?
-->

Obvious drawback that this is confusing. Also, users might expect that changing the system-wide
ringtone has an effect on the app, which won't always be the case.

## Rationale and alternatives

<!--
- Why is this design the best in the space of possible designs?
- What other designs have been considered and what is the rationale for not choosing them?
- What is the impact of not doing this?
-->

Obvious alternative is to not do this. This would mean that users of phones without a default
settings would not be able to select a default ringtone.

## Prior art

<!--
List prior art related to this proposal.
Discuss advantages and disadvantages of existing solutions in production,
which are a valuable source of information on scalability, usability and performance.

For example:
 - Existing open source platforms like Android
 - Existing open source projects
-->

Google DeskClock application does not have a default ringtone picker.

## Future possibilities

<!--
Optional: Describe which possibilities this RFC opens.
Think about what the natural evolution of your proposal would
be and how it could affect the future of platform and project as a whole.
-->