DROP TABLE IF EXISTS user_to_role;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;

CREATE TABLE "users" (
	"id"	INTEGER,
	"login"	TEXT NOT NULL UNIQUE,
	"password"	TEXT,
	"nickname"	TEXT NOT NULL UNIQUE,
	PRIMARY KEY("id" AUTOINCREMENT)
);

CREATE TABLE roles (
	id	INTEGER NOT NULL UNIQUE,
	name	TEXT NOT NULL UNIQUE,
	PRIMARY KEY(id AUTOINCREMENT)
);

CREATE TABLE user_to_role (
	user_id	INTEGER,
	role_id	INTEGER,
	CONSTRAINT user_id_fk FOREIGN KEY(user_id)  REFERENCES users(id) ON DELETE CASCADE,
	CONSTRAINT role_id_fk FOREIGN KEY(role_id)  REFERENCES roles(id)
);

DROP TRIGGER IF EXISTS insert_into_users_to_role_after_insert_users;
CREATE TRIGGER insert_into_users_to_role_after_insert_users 
   AFTER INSERT ON users
BEGIN
   INSERT INTO user_to_role(user_id, role_id) VALUES(NEW.id,(SELECT MAX(id) FROM roles r WHERE r.name = "USER"));
END;

delete from sqlite_sequence;
insert into sqlite_sequence(name,seq) values("roles", 0); 
insert into sqlite_sequence(name,seq) values("users", 0);

insert into roles(name) values("ADMIN");
insert into roles(name) values("USER");

insert into users(login, password, nickname) values("sa", "master", "Admin");
insert into users(login, password, nickname) values("login1", "pass1", "User1");
insert into users(login, password, nickname) values("login2", "pass2", "User2");
insert into users(login, password, nickname) values("login3", "pass3", "User3");
insert into users(login, password, nickname) values("login4", "pass4", "User4");
insert into users(login, password, nickname) values("login5", "pass5", "User5");
insert into users(login, password, nickname) values("login6", "pass6", "User6");
insert into users(login, password, nickname) values("login7", "pass7", "User7");
insert into users(login, password, nickname) values("login8", "pass8", "User8");
insert into users(login, password, nickname) values("login9", "pass9", "User9");
insert into users(login, password, nickname) values("login10", "pass10", "User10");
update user_to_role
 set role_id = 1
where user_id = 1;
