CREATE SEQUENCE movie_seq
    START WITH 1
    INCREMENT BY 1;

CREATE SEQUENCE producer_seq
    START WITH 1
    INCREMENT BY 1;


CREATE TABLE movie (
    id BIGINT NOT NULL DEFAULT NEXT VALUE FOR movie_seq,
    year INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    winner BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_movie
        PRIMARY KEY (id)
);


CREATE TABLE producer (
    id BIGINT NOT NULL DEFAULT NEXT VALUE FOR producer_seq,
    name VARCHAR(255) NOT NULL,

    CONSTRAINT pk_producer
        PRIMARY KEY (id),

    CONSTRAINT uk_producer_name
        UNIQUE (name)
);


CREATE TABLE movie_producer (
    movie_id BIGINT NOT NULL,
    producer_id BIGINT NOT NULL,

    CONSTRAINT pk_movie_producer
        PRIMARY KEY (movie_id, producer_id),

    CONSTRAINT fk_movie_producer_movie
        FOREIGN KEY (movie_id)
        REFERENCES movie (id),

    CONSTRAINT fk_movie_producer_producer
        FOREIGN KEY (producer_id)
        REFERENCES producer (id)
);


CREATE INDEX idx_movie_year_winner
    ON movie (year, winner);

CREATE INDEX idx_movie_producer_producer
    ON movie_producer (producer_id);
