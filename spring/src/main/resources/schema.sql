-- Schema for H2 and MySQL

create table test_case (
    id bigint not null auto_increment,

    created timestamp,
    updated timestamp,

    name varchar(255) unique,
    description varchar(1024),

    primary key (id)
);

create table run (
    id bigint not null auto_increment,
    parent_id bigint,

    created timestamp,
    updated timestamp,

    version varchar(32),
    class_name varchar(255),
    checksum varchar(255),
    status integer,
    baseline boolean not null,

    primary key (id),
    foreign key (parent_id) references test_case(id) on delete cascade
);

create table job (
    id bigint not null auto_increment,
    parent_id bigint not null,

    created timestamp,
    updated timestamp,

    client_number integer not null,
    host varchar(255),
    symbolic_name varchar(255),
    details varchar(1024),

    primary key (id),
    foreign key (parent_id) references run(id) on delete cascade
);

create table payload (
    id bigint not null auto_increment,

    created timestamp,
    updated timestamp,

    data MEDIUMBLOB,
    compression_format integer,
    original_length integer not null,

    primary key (id)
);

create table output_log (
    id bigint not null auto_increment,
    parent_id bigint not null,
    payload_id bigint not null,

    created timestamp,
    updated timestamp,

    format varchar(32),
    operation varchar(32),

    primary key (id),
    foreign key (parent_id) references job(id),
    foreign key (payload_id) references payload(id) on delete cascade
);

create table monitor_log (
    id bigint not null auto_increment,
    parent_id bigint not null,
    payload_id bigint not null,

    created timestamp,
    updated timestamp,

    host varchar(255),
    type varchar(32),

    primary key (id),
    foreign key (parent_id) references run(id) on delete cascade,
    foreign key (payload_id) references payload(id) on delete cascade
);