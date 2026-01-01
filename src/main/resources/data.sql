-- Demo Italian 5-letter dictionary
-- NOTE: seed runs only on embedded DBs by default.

INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'pasta');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'pizza');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'gatto');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'amore');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'cuore');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'bello');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'verde');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'rosso');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'acqua');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'cielo');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'notte');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'sedia');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'porta');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'scala');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ANSWER', 'libro');

-- allowed guesses (includes all answers)
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'pasta');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'pizza');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'gatto');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'amore');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'cuore');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'bello');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'verde');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'rosso');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'acqua');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'cielo');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'notte');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'sedia');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'porta');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'scala');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'libro');

-- additional allowed guesses
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'fuoco');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'terra');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'vento');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'maree');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'salto');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'mamma');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'fiore');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'carta');
INSERT INTO dictionary_words(language, type, word) VALUES ('IT', 'ALLOWED', 'penna');
