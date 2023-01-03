db.createUser({
    user: "admin",
    pwd: "admin",
    roles: ["read", "readWrite", "dbAdmin"]
})