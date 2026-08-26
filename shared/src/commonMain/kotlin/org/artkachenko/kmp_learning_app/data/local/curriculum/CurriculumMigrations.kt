package org.artkachenko.kmp_learning_app.data.local.curriculum

import androidx.room3.migration.Migration
import androidx.sqlite.async.executeSQL

internal val MIGRATION_1_2 = Migration(
    startVersion = 1,
    endVersion = 2,
) { connection ->
    connection.executeSQL(
        """
        CREATE TABLE IF NOT EXISTS `test_attempt` (
            `id` TEXT NOT NULL,
            `config_type` TEXT NOT NULL,
            `requested_question_count` INTEGER NOT NULL,
            `scope_type` TEXT,
            `scope_id` TEXT,
            `status` TEXT NOT NULL,
            `score_total_questions` INTEGER,
            `score_correct_answers` INTEGER,
            `started_at_epoch_millis` INTEGER NOT NULL,
            `completed_at_epoch_millis` INTEGER,
            PRIMARY KEY(`id`)
        )
        """,
    )
    connection.executeSQL(
        """
        CREATE TABLE IF NOT EXISTS `question_attempt` (
            `test_attempt_id` TEXT NOT NULL,
            `question_id` TEXT NOT NULL,
            `sort_order` INTEGER NOT NULL,
            `is_correct` INTEGER,
            PRIMARY KEY(`test_attempt_id`, `question_id`),
            FOREIGN KEY(`test_attempt_id`) REFERENCES `test_attempt`(`id`)
                ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`question_id`) REFERENCES `question`(`id`)
                ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """,
    )
    connection.executeSQL(
        """
        CREATE INDEX IF NOT EXISTS `index_question_attempt_question_id`
        ON `question_attempt` (`question_id`)
        """,
    )
    connection.executeSQL(
        """
        CREATE TABLE IF NOT EXISTS `question_attempt_selected_answer` (
            `test_attempt_id` TEXT NOT NULL,
            `question_id` TEXT NOT NULL,
            `answer_id` TEXT NOT NULL,
            PRIMARY KEY(`test_attempt_id`, `question_id`, `answer_id`),
            FOREIGN KEY(`test_attempt_id`, `question_id`)
                REFERENCES `question_attempt`(`test_attempt_id`, `question_id`)
                ON UPDATE NO ACTION ON DELETE NO ACTION,
            FOREIGN KEY(`question_id`, `answer_id`)
                REFERENCES `answer_option`(`question_id`, `id`)
                ON UPDATE NO ACTION ON DELETE NO ACTION
        )
        """,
    )
    connection.executeSQL(
        """
        CREATE INDEX IF NOT EXISTS `index_question_attempt_selected_answer_question_id_answer_id`
        ON `question_attempt_selected_answer` (`question_id`, `answer_id`)
        """,
    )
}
