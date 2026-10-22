# Morning Bro

A daily good-morning ritual for one Discord community spread across cities: a greeting with a coffee, plus an at-a-glance view of each city's weather.

## The message

**Morning Message**:
The single post Morning Bro makes to the Channel: a Greeting Line, a Friday Line on Fridays, and a Weather Report.
_Avoid_: post (as a noun), greeting, bot message

**Greeting Line**:
The first line of the Morning Message: the greeting text followed by the Home City's Condition emoji and any Temperature Alert.
_Avoid_: greeting (for the whole message), header

**Friday Line**:
The extra line that follows the Greeting Line on Fridays only.

**Weather Report**:
The block below the greeting lines that holds one City Forecast per city.
_Avoid_: card, embed, city lines

**City Forecast**:
One city's entry in the Weather Report: flag, name, Condition emoji, any Temperature Alert, low and high temperature, and Rain Chance when it's worth showing.
_Avoid_: city line, entry, tile

**Attribution**:
The credit to the forecast source shown on the Weather Report, as the source's terms require.
_Avoid_: footer

**Fallback Message**:
The Morning Message posted when the weather is unavailable: no Weather Report, no Home City emoji, and the Fallback Note instead.

**Fallback Note**:
The light-hearted line in a Fallback Message saying the weather couldn't be fetched.

## Weather

**Home City**:
The city whose Condition emoji and any Temperature Alert are appended to the Greeting Line.

**Forecast Day**:
The calendar date in the Reference Timezone. Each city's forecast covers that date in the city's own local time, and Fridays are judged by it.
_Avoid_: today (when ambiguous)

**Condition**:
The single summary of a city's sky and precipitation for the Forecast Day, shown as one emoji (Clear, Rain, Fog, Unlikely Rain, …).
_Avoid_: weather code, override, special weather

**Unlikely Rain**:
The Condition shown when rain or a thunderstorm is forecast but the Rain Chance is below the Unlikely Rain Threshold.

**Temperature Alert**:
An extra emoji shown after the Condition when a city's temperature is extreme for the Forecast Day: Hot or Freezing.
_Avoid_: override, special weather, warning

**Hot**:
The Temperature Alert for a day whose high is 35° or more.
_Avoid_: very hot

**Freezing**:
The Temperature Alert for a day whose low is 0° or below.

**Rain Chance**:
The day's highest chance of rain for a city, shown as "☔ NN%".
_Avoid_: rain probability, precipitation probability, chance of rain

**Unlikely Rain Threshold**:
The Rain Chance below which forecast rain or a thunderstorm becomes Unlikely Rain.

**Rain Chance Display Threshold**:
The Rain Chance at or above which it is shown in a City Forecast.

## Scheduling

**Post Time**:
The hour of the day, in the Reference Timezone, at which the Scheduled Post goes out.
_Avoid_: send time, send hour, send window

**Reference Timezone**:
The timezone that defines the Post Time and the Forecast Day.

**Scheduled Post**:
A Morning Message posted automatically at the Post Time. There is at most one per Forecast Day.

**Forced Post**:
A Morning Message the Owner triggers on demand, regardless of the Post Time. It is independent of Scheduled Posts: it neither counts toward nor prevents one.
_Avoid_: manual run, test post

**Dry Run**:
A run that builds the Morning Message and shows it to the Owner instead of posting it to the Channel.
_Avoid_: test post, preview

## People and places

**Owner**:
The person who configures and runs Morning Bro and is notified when something fails.

**Member**:
A person who reads the Morning Message in the Channel.
_Avoid_: user, subscriber

**Channel**:
The one Discord channel Morning Bro posts to.

**Morning Bro**:
The named identity the Morning Message is posted under.
_Avoid_: bot

**Settings File**:
The single human-readable file where the Owner sets cities, texts, emoji, thresholds, Post Time and identity. Secrets never live in it.
_Avoid_: config file
