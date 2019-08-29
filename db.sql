create table profiles
(
    address     VARCHAR(42)
        constraint profiles_pk
            primary key,
    description TEXT,
    www         TEXT,
    icon        TEXT,
    isVerified  boolean,
    title       text,
    fish        text
);

create table transactions
(
    hash    VARCHAR(64) not null
        constraint transactions_pk
            primary key,
    block   INT         not null,
    a_from  VARCHAR(42) not null,
    a_to    VARCHAR(42) not null,
    amount  NUMERIC(18) default 0 not null,
    payload INT,
    date    DATETIME,
    nonce   INT         default 0 not null
);