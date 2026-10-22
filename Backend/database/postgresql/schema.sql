-- PostgreSQL 17+ schema matching the current application JPA mappings.
-- The migration tool applies this only when all application tables are absent.

CREATE SCHEMA IF NOT EXISTS public;





CREATE TABLE public.deliveries (
    delivery_id bigint NOT NULL,
    ball_num integer,
    extra_type character varying(50),
    innings integer,
    over_num integer,
    runs_batter integer,
    runs_extras integer,
    wicket_type character varying(50),
    batter_id bigint,
    bowler_id bigint,
    match_id integer,
    non_striker_id bigint,
    player_dismissed_id bigint
);



CREATE SEQUENCE public.deliveries_delivery_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.deliveries_delivery_id_seq OWNED BY public.deliveries.delivery_id;



CREATE TABLE public.match_squads (
    match_id integer NOT NULL,
    player_id bigint NOT NULL,
    is_captain boolean,
    is_keeper boolean,
    is_playing boolean,
    team_id integer
);



CREATE TABLE public.matches (
    match_id integer NOT NULL,
    format character varying(10),
    match_date date,
    result_margin integer,
    result_type character varying(255),
    toss_decision character varying(255),
    venue character varying(100),
    player_of_match bigint,
    season_id integer,
    team_a_id integer,
    team_b_id integer,
    toss_winner_id integer,
    winner_id integer,
    CONSTRAINT matches_format_check CHECK (((format)::text = ANY ((ARRAY['T20'::character varying, 'ODI'::character varying, 'Test'::character varying, 'SUPEROVER'::character varying])::text[]))),
    CONSTRAINT matches_result_type_check CHECK (((result_type)::text = ANY ((ARRAY['runs'::character varying, 'wickets'::character varying, 'tie'::character varying, 'no_result'::character varying])::text[]))),
    CONSTRAINT matches_toss_decision_check CHECK (((toss_decision)::text = ANY ((ARRAY['bat'::character varying, 'bowl'::character varying])::text[])))
);



CREATE SEQUENCE public.matches_match_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.matches_match_id_seq OWNED BY public.matches.match_id;



CREATE TABLE public.partnerships (
    partnership_id bigint NOT NULL,
    balls_faced integer,
    innings integer,
    runs integer,
    wicket_number integer,
    batter1_id bigint,
    batter2_id bigint,
    match_id integer
);



CREATE SEQUENCE public.partnerships_partnership_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.partnerships_partnership_id_seq OWNED BY public.partnerships.partnership_id;



CREATE TABLE public.player (
    id bigint NOT NULL,
    age integer,
    bowling_style character varying(255),
    first_name character varying(255),
    full_name character varying(255),
    last_name character varying(255),
    nationality character varying(255),
    playing_style character varying(255),
    role character varying(255) NOT NULL,
    CONSTRAINT player_age_check CHECK (((age <= 99) AND (age >= 15))),
    CONSTRAINT player_bowling_style_check CHECK (((bowling_style)::text = ANY ((ARRAY['FastBaller'::character varying, 'FastMedium'::character varying, 'MediumFast'::character varying, 'MediumPacer'::character varying, 'SlowMediumPacer'::character varying, 'OffSpinner'::character varying, 'LegSpinner'::character varying, 'LeftArmOrthodox'::character varying, 'LeftArmWristSpin'::character varying])::text[]))),
    CONSTRAINT player_playing_style_check CHECK (((playing_style)::text = ANY ((ARRAY['Right'::character varying, 'Left'::character varying])::text[]))),
    CONSTRAINT player_role_check CHECK (((role)::text = ANY ((ARRAY['Batsman'::character varying, 'Baller'::character varying, 'Keeper'::character varying, 'Allrounder'::character varying])::text[])))
);



CREATE SEQUENCE public.player_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.player_id_seq OWNED BY public.player.id;



CREATE TABLE public.scorecard_batters (
    match_id integer NOT NULL,
    player_id bigint NOT NULL,
    balls_faced integer,
    batting_position integer,
    fours integer,
    is_out boolean,
    out_description character varying(255),
    runs integer,
    sixes integer
);



CREATE TABLE public.scorecard_bowlers (
    match_id integer NOT NULL,
    player_id bigint NOT NULL,
    economy numeric(4,2),
    maidens integer,
    overs_bowled numeric(3,1),
    runs_conceded integer,
    wickets integer
);



CREATE TABLE public.season (
    season_id integer NOT NULL,
    season_year integer
);



CREATE SEQUENCE public.season_season_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.season_season_id_seq OWNED BY public.season.season_id;



CREATE TABLE public.teams (
    team_id integer NOT NULL,
    team_name character varying(100) NOT NULL
);



CREATE SEQUENCE public.teams_team_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



ALTER SEQUENCE public.teams_team_id_seq OWNED BY public.teams.team_id;



ALTER TABLE ONLY public.deliveries ALTER COLUMN delivery_id SET DEFAULT nextval('public.deliveries_delivery_id_seq'::regclass);



ALTER TABLE ONLY public.matches ALTER COLUMN match_id SET DEFAULT nextval('public.matches_match_id_seq'::regclass);



ALTER TABLE ONLY public.partnerships ALTER COLUMN partnership_id SET DEFAULT nextval('public.partnerships_partnership_id_seq'::regclass);



ALTER TABLE ONLY public.player ALTER COLUMN id SET DEFAULT nextval('public.player_id_seq'::regclass);



ALTER TABLE ONLY public.season ALTER COLUMN season_id SET DEFAULT nextval('public.season_season_id_seq'::regclass);



ALTER TABLE ONLY public.teams ALTER COLUMN team_id SET DEFAULT nextval('public.teams_team_id_seq'::regclass);



ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT deliveries_pkey PRIMARY KEY (delivery_id);



ALTER TABLE ONLY public.match_squads
    ADD CONSTRAINT match_squads_pkey PRIMARY KEY (match_id, player_id);



ALTER TABLE ONLY public.matches
    ADD CONSTRAINT matches_pkey PRIMARY KEY (match_id);



ALTER TABLE ONLY public.partnerships
    ADD CONSTRAINT partnerships_pkey PRIMARY KEY (partnership_id);



ALTER TABLE ONLY public.player
    ADD CONSTRAINT player_pkey PRIMARY KEY (id);



ALTER TABLE ONLY public.scorecard_batters
    ADD CONSTRAINT scorecard_batters_pkey PRIMARY KEY (match_id, player_id);



ALTER TABLE ONLY public.scorecard_bowlers
    ADD CONSTRAINT scorecard_bowlers_pkey PRIMARY KEY (match_id, player_id);



ALTER TABLE ONLY public.season
    ADD CONSTRAINT season_pkey PRIMARY KEY (season_id);



ALTER TABLE ONLY public.teams
    ADD CONSTRAINT teams_pkey PRIMARY KEY (team_id);



CREATE INDEX idx_delivery_matchup_batter_bowler ON public.deliveries USING btree (batter_id, bowler_id, match_id);



CREATE INDEX idx_delivery_matchup_bowler_batter ON public.deliveries USING btree (bowler_id, batter_id, match_id);



ALTER TABLE ONLY public.match_squads
    ADD CONSTRAINT fk5jcbdt0sc4wtv2yegp7wfrdgl FOREIGN KEY (match_id) REFERENCES public.matches(match_id);



ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT fk728mbq742kjkuf363a44n8udw FOREIGN KEY (batter_id) REFERENCES public.player(id);



ALTER TABLE ONLY public.scorecard_batters
    ADD CONSTRAINT fk7u40j5wrv3e7r8ms6jn77m6jn FOREIGN KEY (match_id) REFERENCES public.matches(match_id);



ALTER TABLE ONLY public.scorecard_bowlers
    ADD CONSTRAINT fk8h2q3pltga4j163kcxqctf0ui FOREIGN KEY (match_id) REFERENCES public.matches(match_id);



ALTER TABLE ONLY public.matches
    ADD CONSTRAINT fkajd8awp9pr01v8vstmcpbt37d FOREIGN KEY (player_of_match) REFERENCES public.player(id);



ALTER TABLE ONLY public.matches
    ADD CONSTRAINT fkbeel88lh2ksupphafotnqy7ry FOREIGN KEY (winner_id) REFERENCES public.teams(team_id);



ALTER TABLE ONLY public.partnerships
    ADD CONSTRAINT fkd91006sa6sgyio9ahdgnhbygw FOREIGN KEY (batter2_id) REFERENCES public.player(id);



ALTER TABLE ONLY public.partnerships
    ADD CONSTRAINT fkehxh6nt2gd2bkk5u3feaoouih FOREIGN KEY (batter1_id) REFERENCES public.player(id);



ALTER TABLE ONLY public.scorecard_bowlers
    ADD CONSTRAINT fkgqp90s54gk673xc870k4orf4s FOREIGN KEY (player_id) REFERENCES public.player(id);



ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT fkgvhhv0mgsom9o74n8vhp54w0f FOREIGN KEY (player_dismissed_id) REFERENCES public.player(id);



ALTER TABLE ONLY public.scorecard_batters
    ADD CONSTRAINT fkis5sldve7incpdlhhajrq2ys8 FOREIGN KEY (player_id) REFERENCES public.player(id);



ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT fkisl5b0cig1w0rj2ubsoc786h4 FOREIGN KEY (non_striker_id) REFERENCES public.player(id);



ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT fkkyplyrq20r446q98h25impe74 FOREIGN KEY (bowler_id) REFERENCES public.player(id);



ALTER TABLE ONLY public.matches
    ADD CONSTRAINT fkmrgum5di41ywbw87fdecijmvs FOREIGN KEY (team_a_id) REFERENCES public.teams(team_id);



ALTER TABLE ONLY public.matches
    ADD CONSTRAINT fkntr6m7io9k4xcxgn7prtyojko FOREIGN KEY (team_b_id) REFERENCES public.teams(team_id);



ALTER TABLE ONLY public.deliveries
    ADD CONSTRAINT fkpcsj53xsrfij1gda0n1qlnok6 FOREIGN KEY (match_id) REFERENCES public.matches(match_id);



ALTER TABLE ONLY public.matches
    ADD CONSTRAINT fkpf41hapqtm6mc2t8xnr7ei4b0 FOREIGN KEY (toss_winner_id) REFERENCES public.teams(team_id);



ALTER TABLE ONLY public.match_squads
    ADD CONSTRAINT fkq9ie1pcla74ixrier0i2rvd6w FOREIGN KEY (team_id) REFERENCES public.teams(team_id);



ALTER TABLE ONLY public.match_squads
    ADD CONSTRAINT fks7clgbt94ef5gdfiafoba3cdr FOREIGN KEY (player_id) REFERENCES public.player(id);



ALTER TABLE ONLY public.partnerships
    ADD CONSTRAINT fksojm93mq2x5ilio3lol48qo8m FOREIGN KEY (match_id) REFERENCES public.matches(match_id);



ALTER TABLE ONLY public.matches
    ADD CONSTRAINT fkt32wb9qvxjjj8rtbciiwurdyv FOREIGN KEY (season_id) REFERENCES public.season(season_id);
