--
-- PostgreSQL database dump
--

\restrict 13j7xYi3ib29edwRkwcnZaWjF8dp7AIhcugG7C9bjJbnHgo5FUwEioLYMxeaYNg

-- Dumped from database version 18.1
-- Dumped by pg_dump version 18.1

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: public; Type: SCHEMA; Schema: -; Owner: postgres
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO postgres;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: postgres
--

COMMENT ON SCHEMA public IS '';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: transactions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.transactions (
    id bigint NOT NULL,
    wallet_id bigint NOT NULL,
    type character varying(10) NOT NULL,
    amount numeric(19,2) NOT NULL,
    balance_after numeric(19,2) NOT NULL,
    counterparty_wallet_id bigint,
    reference_id character varying(50) NOT NULL,
    status character varying(20) NOT NULL,
    description character varying(255),
    created_at timestamp with time zone NOT NULL,
    idempotency_key character varying(100),
    operation character varying(20),
    CONSTRAINT chk_transactions_operation CHECK (((operation IS NULL) OR ((operation)::text = ANY ((ARRAY['DEPOSIT'::character varying, 'TRANSFER'::character varying])::text[])))),
    CONSTRAINT chk_transactions_status CHECK (((status)::text = ANY ((ARRAY['COMPLETED'::character varying, 'PENDING'::character varying, 'FAILED'::character varying])::text[]))),
    CONSTRAINT chk_transactions_type CHECK (((type)::text = ANY ((ARRAY['CREDIT'::character varying, 'DEBIT'::character varying])::text[])))
);


ALTER TABLE public.transactions OWNER TO postgres;

--
-- Name: transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.transactions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.transactions_id_seq OWNER TO postgres;

--
-- Name: transactions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.transactions_id_seq OWNED BY public.transactions.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    created_at timestamp with time zone NOT NULL
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: wallets; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.wallets (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    balance numeric(19,2) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.wallets OWNER TO postgres;

--
-- Name: wallets_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.wallets_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.wallets_id_seq OWNER TO postgres;

--
-- Name: wallets_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.wallets_id_seq OWNED BY public.wallets.id;


--
-- Name: transactions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions ALTER COLUMN id SET DEFAULT nextval('public.transactions_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: wallets id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallets ALTER COLUMN id SET DEFAULT nextval('public.wallets_id_seq'::regclass);


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	create users table	SQL	V1__create_users_table.sql	-243804795	postgres	2026-08-26 14:36:00.593699	113	t
2	2	create wallets table	SQL	V2__create_wallets_table.sql	-330253672	postgres	2026-08-26 14:36:00.837415	126	t
3	3	create transactions table	SQL	V3__create_transactions_table.sql	409835095	postgres	2026-08-26 14:36:01.020187	9	t
4	4	add idempotency key to transactions	SQL	V4__add_idempotency_key_to_transactions.sql	-1815823165	postgres	2026-08-26 18:55:17.666601	938	t
5	5	scope idempotency key	SQL	V5__scope_idempotency_key.sql	1948542416	postgres	2026-08-27 16:31:19.547958	2879	t
\.


--
-- Data for Name: transactions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.transactions (id, wallet_id, type, amount, balance_after, counterparty_wallet_id, reference_id, status, description, created_at, idempotency_key, operation) FROM stdin;
1	2	CREDIT	200.00	200.00	\N	DEP-20260827-DD5F6805	COMPLETED	\N	2026-08-27 19:02:15.410489+03	550e8400-e29b-41d4-a716-446655440001	DEPOSIT
2	1	CREDIT	200.00	200.00	\N	DEP-20260827-67510D8C	COMPLETED	\N	2026-08-27 19:02:32.937895+03	550e8400-e29b-41d4-a716-446655440011	DEPOSIT
3	1	DEBIT	100.00	100.00	2	TX-20260827-76098894	COMPLETED	\N	2026-08-27 19:02:53.014096+03	550e8400-e29b-41d4-a716-446655440111	TRANSFER
4	2	CREDIT	100.00	300.00	1	TX-20260827-76098894	COMPLETED	\N	2026-08-27 19:02:53.022215+03	\N	TRANSFER
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, username, email, created_at) FROM stdin;
1	ahmed	ahmed@example.com	2026-08-27 19:01:38.132019+03
2	sara	sara@example.com	2026-08-27 19:01:38.223012+03
3	ali	ali@example.com	2026-08-27 19:01:38.22702+03
4	omar	omar@example.com	2026-08-27 19:01:38.231285+03
5	abdullah	abdullah@example.com	2026-08-27 19:01:38.235119+03
\.


--
-- Data for Name: wallets; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.wallets (id, user_id, balance, created_at, updated_at) FROM stdin;
1	1	100.00	2026-08-27 19:01:56.345428+03	2026-08-27 19:02:53.017089+03
2	2	300.00	2026-08-27 19:02:02.381347+03	2026-08-27 19:02:53.018159+03
\.


--
-- Name: transactions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.transactions_id_seq', 4, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 5, true);


--
-- Name: wallets_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.wallets_id_seq', 2, true);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);


--
-- Name: transactions uk_transactions_wallet_operation_idempotency; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT uk_transactions_wallet_operation_idempotency UNIQUE (wallet_id, operation, idempotency_key);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: wallets wallets_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallets
    ADD CONSTRAINT wallets_pkey PRIMARY KEY (id);


--
-- Name: wallets wallets_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallets
    ADD CONSTRAINT wallets_user_id_key UNIQUE (user_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_transactions_wallet_created; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_transactions_wallet_created ON public.transactions USING btree (wallet_id, created_at DESC);


--
-- Name: transactions fk_transactions_counterparty_wallet; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT fk_transactions_counterparty_wallet FOREIGN KEY (counterparty_wallet_id) REFERENCES public.wallets(id);


--
-- Name: transactions fk_transactions_wallet; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT fk_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES public.wallets(id);


--
-- Name: wallets fk_wallets_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.wallets
    ADD CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


--
-- PostgreSQL database dump complete
--

\unrestrict 13j7xYi3ib29edwRkwcnZaWjF8dp7AIhcugG7C9bjJbnHgo5FUwEioLYMxeaYNg

