# PocketStats

App for visualising [GetPocket](https://getpocket.com/) usage stats

## Config

### Backend

#### Environment variables
- `BACKEND_URL` - backend url, default: http://localhost:8080
- `POCKET_CONSUMER_KEY` - get pocket consumer key
- `MONGODB_HOST` - mongo db host 
- `MONGODB_URI` - full mongodb uri with login and pass

example env variables for local instance workig with dockerized MongoDB
```
{
  "VUE_APP_BACKEND_URL": "http://127.0.0.1:8081/pocketstats",
  "POCKET_CONSUMER_KEY": "[get pocket consumer key]",
  "MONGODB_DB": "test",
  "MONGODB_URI": "mongodb+srv://db-user:mnj0pYRuglcOKvl5@cluster0.wdgepgr.mongodb.net/?retryWrites=true&w=majority"
}
```

#### Database
Docker service file with mongodb can be find here: [mongodb.yml](https://github.com/michmzr/PocketStats/blob/master/src/main/docker/mongodb.yml)



### Frontend 
#### Environment variables
- `VUE_APP_BACKEND_URL` - java backend url

**You can read more in
article**: [https://cybershu.eu/articles/pocket-stats-analytics-app-get-pocket-api.html](https://cybershu.eu/articles/pocket-stats-analytics-app-get-pocket-api.html) (it describes old version!)
