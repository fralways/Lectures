--
-- PostgreSQL database dump
--

-- Dumped from database version 9.6.2
-- Dumped by pg_dump version 9.6.2

-- Started on 2017-02-21 12:27:00

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 2159 (class 1262 OID 12401)
-- Dependencies: 2158
-- Name: postgres; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON DATABASE postgres IS 'default administrative connection database';


--
-- TOC entry 8 (class 2615 OID 16402)
-- Name: lectures; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA lectures;


ALTER SCHEMA lectures OWNER TO postgres;

--
-- TOC entry 2 (class 3079 OID 12387)
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- TOC entry 2160 (class 0 OID 0)
-- Dependencies: 2
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- TOC entry 1 (class 3079 OID 16384)
-- Name: adminpack; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS adminpack WITH SCHEMA pg_catalog;


--
-- TOC entry 2161 (class 0 OID 0)
-- Dependencies: 1
-- Name: EXTENSION adminpack; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION adminpack IS 'administrative functions for PostgreSQL';


--
-- TOC entry 3 (class 3079 OID 16437)
-- Name: uuid-ossp; Type: EXTENSION; Schema: -; Owner: 
--

CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA lectures;


--
-- TOC entry 2162 (class 0 OID 0)
-- Dependencies: 3
-- Name: EXTENSION "uuid-ossp"; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION "uuid-ossp" IS 'generate universally unique identifiers (UUIDs)';


SET search_path = lectures, pg_catalog;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- TOC entry 188 (class 1259 OID 16411)
-- Name: image; Type: TABLE; Schema: lectures; Owner: postgres
--

CREATE TABLE image (
    id text NOT NULL,
    defaultimage boolean,
    userref text,
    image bytea
);


ALTER TABLE image OWNER TO postgres;

--
-- TOC entry 189 (class 1259 OID 16419)
-- Name: lecture; Type: TABLE; Schema: lectures; Owner: postgres
--

CREATE TABLE lecture (
    guid text NOT NULL,
    title text,
    description text,
    unique_id text,
    owner text,
    questions text[]
);


ALTER TABLE lecture OWNER TO postgres;

--
-- TOC entry 190 (class 1259 OID 16429)
-- Name: question; Type: TABLE; Schema: lectures; Owner: postgres
--

CREATE TABLE question (
    guid text NOT NULL,
    question text,
    correctindex integer,
    duration double precision,
    answers text[],
    owner text
);


ALTER TABLE question OWNER TO postgres;

--
-- TOC entry 187 (class 1259 OID 16403)
-- Name: users; Type: TABLE; Schema: lectures; Owner: postgres
--

CREATE TABLE users (
    email text NOT NULL,
    title text,
    user_id text,
    firstname text,
    lastname text,
    description text,
    university text,
    image_id text,
    password text,
    guid text NOT NULL,
    lectures text[],
    runninglecture text
);


ALTER TABLE users OWNER TO postgres;

--
-- TOC entry 2030 (class 2606 OID 16418)
-- Name: image image_pkey; Type: CONSTRAINT; Schema: lectures; Owner: postgres
--

ALTER TABLE ONLY image
    ADD CONSTRAINT image_pkey PRIMARY KEY (id);


--
-- TOC entry 2032 (class 2606 OID 16426)
-- Name: lecture lecture_pkey; Type: CONSTRAINT; Schema: lectures; Owner: postgres
--

ALTER TABLE ONLY lecture
    ADD CONSTRAINT lecture_pkey PRIMARY KEY (guid);


--
-- TOC entry 2034 (class 2606 OID 16428)
-- Name: lecture lecture_unique_id_key; Type: CONSTRAINT; Schema: lectures; Owner: postgres
--

ALTER TABLE ONLY lecture
    ADD CONSTRAINT lecture_unique_id_key UNIQUE (unique_id);


--
-- TOC entry 2036 (class 2606 OID 16436)
-- Name: question question_pkey; Type: CONSTRAINT; Schema: lectures; Owner: postgres
--

ALTER TABLE ONLY question
    ADD CONSTRAINT question_pkey PRIMARY KEY (guid);


--
-- TOC entry 2028 (class 2606 OID 16410)
-- Name: users users_pkey; Type: CONSTRAINT; Schema: lectures; Owner: postgres
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (guid);


-- Completed on 2017-02-21 12:27:00

--
-- PostgreSQL database dump complete
--

