-- 插入世界模板数据
INSERT IGNORE INTO `world_templates` (`world_id`, `world_name`, `description`, `system_prompt_template`, `default_rules`, `character_templates`, `location_templates`, `quest_templates`, `stability_anchors`, `convergence_scenarios`, `dm_instructions`, `convergence_rules`) VALUES
('fantasy_adventure', '异世界探险', '经典的奇幻冒险世界，充满魔法、怪物和宝藏',
 '你是一个奇幻世界的游戏主持人。这个世界充满了魔法、神秘生物和古老的传说。用户是一名冒险者，你需要为他们创造引人入胜的冒险故事。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('magic_system', '经典魔法体系', 'technology_level', '中世纪', 'danger_level', '中等'),
 JSON_OBJECT(
   'professions', JSON_ARRAY(
     JSON_OBJECT('id', 'warrior', 'name', '战士', 'description', '近战专家，擅长剑术和防御'),
     JSON_OBJECT('id', 'mage', 'name', '法师', 'description', '魔法大师，掌握各种法术'),
     JSON_OBJECT('id', 'rogue', 'name', '盗贼', 'description', '敏捷灵活，擅长潜行和暗杀'),
     JSON_OBJECT('id', 'cleric', 'name', '牧师', 'description', '神圣治疗者，驱散邪恶')
   ),
   'skills', JSON_ARRAY(
     JSON_OBJECT('id', 'sword_mastery', 'name', '剑术精通', 'description', '提高近战攻击力'),
     JSON_OBJECT('id', 'magic_affinity', 'name', '魔法亲和', 'description', '增强法术效果'),
     JSON_OBJECT('id', 'stealth', 'name', '潜行', 'description', '降低被发现的概率'),
     JSON_OBJECT('id', 'healing', 'name', '治疗术', 'description', '恢复生命值'),
     JSON_OBJECT('id', 'archery', 'name', '弓箭术', 'description', '远程攻击技能'),
     JSON_OBJECT('id', 'alchemy', 'name', '炼金术', 'description', '制作药剂和魔法物品'),
     JSON_OBJECT('id', 'beast_taming', 'name', '野兽驯服', 'description', '与动物沟通'),
     JSON_OBJECT('id', 'lockpicking', 'name', '开锁术', 'description', '打开各种锁具')
   )
 ),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'village_crisis',
     'title', '村庄危机',
     'description', '玩家所在的村庄遭受怪物袭击，发现远古预言',
     'trigger_conditions', JSON_ARRAY('explored_starting_area', 'encountered_first_enemy'),
     'required_elements', JSON_ARRAY('player_character', 'village', 'ancient_scroll'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('discover_prophecy', 'village_saved', 'mysterious_survivor')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'ancient_temple',
     'title', '远古神庙',
     'description', '根据预言前往远古神庙，寻找失落的魔法物品',
     'trigger_conditions', JSON_ARRAY('completed_village_quest', 'gathered_party_members'),
     'required_elements', JSON_ARRAY('magic_artifact', 'temple_guardians', 'puzzles'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('artifact_found', 'guardian_defeated', 'temple_collapse')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'dragon_lair',
     'title', '巨龙巢穴',
     'description', '追踪魔法物品的源头，来到巨龙巢穴',
     'trigger_conditions', JSON_ARRAY('found_ancient_temple', 'decoded_prophecy'),
     'required_elements', JSON_ARRAY('dragon', 'magic_sword', 'treasure_hoard'),
     'leads_to', 'story_convergence_4',
     'outcomes', JSON_ARRAY('dragon_encountered', 'treasure_discovered', 'lair_explored')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'dragon_slaying',
     'title', '屠龙英雄',
     'description', '击败巨龙，成为传说中的英雄，终结预言中的灾难',
     'trigger_conditions', JSON_ARRAY('entered_dragon_lair', 'prepared_for_final_battle'),
     'required_elements', JSON_ARRAY('hero', 'dragon', 'magic_sword'),
     'outcomes', JSON_ARRAY('victory', 'defeat', 'compromise')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'kingdom_savior',
     'title', '王国救世主',
     'description', '拯救王国于危难之中，建立新的秩序',
     'trigger_conditions', JSON_ARRAY('player_chose_diplomatic_approach', 'maintained_peace_long_enough')
   )
 ),
 '{}',
 '{}',
 '{}',
 '作为奇幻世界的DM，你需要平衡魔法与现实，鼓励英雄主义行为，同时确保故事的连贯性和趣味性。',
 JSON_OBJECT('convergence_threshold', 0.8, 'max_exploration_turns', 50, 'story_completeness_required', 0.7)),

('western_magic', '西方魔幻', '西式魔法世界，包含法师、骑士和龙',
 '你是西方魔幻世界的向导。这里有强大的法师、勇敢的骑士、神秘的龙族和各种魔法生物。为用户创造史诗般的冒险。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('magic_schools', JSON_ARRAY('元素魔法', '神圣魔法', '暗黑魔法'), 'guilds', true, 'dragons', true),
 JSON_OBJECT(
   'professions', JSON_ARRAY(
     JSON_OBJECT('id', 'knight', 'name', '骑士', 'description', '荣誉的守护者，精通剑术'),
     JSON_OBJECT('id', 'wizard', 'name', '巫师', 'description', '古老的魔法传承者'),
     JSON_OBJECT('id', 'ranger', 'name', '游侠', 'description', '森林的守护者，擅长弓箭'),
     JSON_OBJECT('id', 'paladin', 'name', '圣骑士', 'description', '神圣与正义的化身')
   ),
   'skills', JSON_ARRAY(
     JSON_OBJECT('id', 'holy_magic', 'name', '神圣魔法', 'description', '驱散邪恶的神圣力量'),
     JSON_OBJECT('id', 'elemental_magic', 'name', '元素魔法', 'description', '操控火、水、土、风'),
     JSON_OBJECT('id', 'sword_techniques', 'name', '剑技', 'description', '精妙的剑术招式'),
     JSON_OBJECT('id', 'nature_lore', 'name', '自然知识', 'description', '了解动植物和草药'),
     JSON_OBJECT('id', 'divine_protection', 'name', '神圣护佑', 'description', '抵御邪恶攻击'),
     JSON_OBJECT('id', 'beast_communication', 'name', '野兽沟通', 'description', '与动物交流')
   )
 ),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'academy_enrollment',
     'title', '魔法学院入学',
     'description', '玩家进入著名的魔法学院，开始学习魔法',
     'trigger_conditions', JSON_ARRAY('showed_magical_talent', 'passed_entrance_exam'),
     'required_elements', JSON_ARRAY('student_mage', 'spellbook', 'wand'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('accepted_into_academy', 'met_rival_student', 'discovered_hidden_talent')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'forbidden_library',
     'title', '禁忌图书馆',
     'description', '意外发现学院的禁忌图书馆，接触失传的魔法',
     'trigger_conditions', JSON_ARRAY('advanced_in_studies', 'broke_academy_rules'),
     'required_elements', JSON_ARRAY('forbidden_knowledge', 'ancient_tomes', 'magical_traps'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('knowledge_gained', 'curse_acquired', 'professor_suspicion')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'dragon_sanctuary',
     'title', '龙族圣殿',
     'description', '根据古籍中的线索，寻找传说中的龙族圣殿',
     'trigger_conditions', JSON_ARRAY('decoded_ancient_texts', 'mastered_forbidden_spells'),
     'required_elements', JSON_ARRAY('dragon_guardians', 'ancient_artifacts', 'magical_barriers'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('sanctuary_found', 'dragon_alliance', 'magical_awakening')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'archmage_trial',
     'title', '大法师试炼',
     'description', '通过魔法学院的最终试炼，成为大法师',
     'trigger_conditions', JSON_ARRAY('player_mastered_magic', 'completed_academy_quests'),
     'required_elements', JSON_ARRAY('mage', 'spellbook', 'magic_crystal'),
     'outcomes', JSON_ARRAY('ascension', 'failure', 'alternative_path')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'dragon_peace',
     'title', '龙族和平',
     'description', '与龙族达成和平协议，建立魔法与龙族的联盟',
     'trigger_conditions', JSON_ARRAY('player_negotiated_with_dragons', 'prevented_war')
   )
 ),
 '{}',
 '{}',
 '{}',
 '作为西方魔幻世界的DM，你需要维护魔法世界的平衡，鼓励玩家探索不同的魔法流派，同时引导故事向史诗般的结局发展。',
 JSON_OBJECT('convergence_threshold', 0.75, 'max_exploration_turns', 40, 'story_completeness_required', 0.8)),

('martial_arts', '东方武侠', '充满武功、江湖恩怨的武侠世界',
 '你是武侠世界的说书人。这里有各种武功秘籍、江湖门派、侠客义士。用户将体验刀光剑影的江湖生活。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('martial_arts', true, 'sects', JSON_ARRAY('少林', '武当', '峨眉'), 'weapons', JSON_ARRAY('剑', '刀', '拳法')),
 JSON_OBJECT(
   'professions', JSON_ARRAY(
     JSON_OBJECT('id', 'swordsman', 'name', '剑客', 'description', '以剑为伴，追求剑道极致'),
     JSON_OBJECT('id', 'monk', 'name', '武僧', 'description', '内外兼修，拳脚功夫了得'),
     JSON_OBJECT('id', 'assassin', 'name', '刺客', 'description', '暗杀专家，身法如鬼魅'),
     JSON_OBJECT('id', 'scholar', 'name', '书生', 'description', '文武双全，以智取胜')
   ),
   'skills', JSON_ARRAY(
     JSON_OBJECT('id', 'sword_art', 'name', '剑法', 'description', '各种精妙剑招'),
     JSON_OBJECT('id', 'internal_energy', 'name', '内功', 'description', '修炼内力，增强体质'),
     JSON_OBJECT('id', 'light_footwork', 'name', '轻功', 'description', '身轻如燕，飞檐走壁'),
     JSON_OBJECT('id', 'poison_resistance', 'name', '毒抗', 'description', '抵抗各种毒素'),
     JSON_OBJECT('id', 'acupuncture', 'name', '点穴', 'description', '精准攻击穴位'),
     JSON_OBJECT('id', 'meditation', 'name', '冥想', 'description', '恢复内力和精神')
   )
 ),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'sect_origin',
     'title', '师门恩怨',
     'description', '玩家出身的门派遭受仇家袭击，揭开家族秘密',
     'trigger_conditions', JSON_ARRAY('completed_basic_training', 'witnessed_attack'),
     'required_elements', JSON_ARRAY('young_warrior', 'sect_members', 'ancient_letter'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('survived_attack', 'discovered_conspiracy', 'met_mentor_figure')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'treasure_hunt',
     'title', '秘籍寻踪',
     'description', '根据线索寻找失传的武功秘籍，游历江湖',
     'trigger_conditions', JSON_ARRAY('left_home_sect', 'gathered_allies'),
     'required_elements', JSON_ARRAY('martial_arts_manual', 'sect_rivals', 'hidden_locations'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('manual_found', 'rival_defeated', 'power_increased')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'grand_tournament',
     'title', '武林大会',
     'description', '参加武林大会，与各派高手一较高下',
     'trigger_conditions', JSON_ARRAY('mastered_secret_techniques', 'gained_reputation'),
     'required_elements', JSON_ARRAY('tournament_competitors', 'legendary_weapon', 'judges_panel'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('tournament_joined', 'alliances_formed', 'secrets_revealed')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'wulin_supremacy',
     'title', '武林至尊',
     'description', '在武林大会上证明自己的武功天下第一，成为武林盟主',
     'trigger_conditions', JSON_ARRAY('player_mastered_martial_arts', 'defeated_all_sects'),
     'required_elements', JSON_ARRAY('warrior', 'legendary_weapon', 'inner_power'),
     'outcomes', JSON_ARRAY('martial_supremacy', 'sect_leader', 'hidden_master')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'jianghu_peace',
     'title', '江湖和平',
     'description', '化解各大门派恩怨，建立武林和平，结束江湖争斗',
     'trigger_conditions', JSON_ARRAY('player_mediated_conflicts', 'united_factions')
   )
 ),
 '{}',
 '{}',
 '{}',
 '作为武侠世界的DM，你需要维护江湖的道义和规矩，鼓励侠义行为，同时引导故事向武林传奇的方向发展。',
 JSON_OBJECT('convergence_threshold', 0.7, 'max_exploration_turns', 35, 'story_completeness_required', 0.75)),

('japanese_school', '日式校园', '现代日本校园生活，充满青春与友情',
 '你是日式校园生活的叙述者。这里有社团活动、校园祭典、青春恋爱和友情故事。为用户创造温馨的校园体验。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('setting', '现代日本', 'school_type', '高中', 'clubs', true, 'festivals', true),
 JSON_OBJECT(
   'professions', JSON_ARRAY(
     JSON_OBJECT('id', 'student_council', 'name', '学生会', 'description', '学校的管理者，组织各种活动'),
     JSON_OBJECT('id', 'athlete', 'name', '运动部', 'description', '体育健将，擅长各种运动'),
     JSON_OBJECT('id', 'art_club', 'name', '美术部', 'description', '艺术天才，创作美丽作品'),
     JSON_OBJECT('id', 'study_group', 'name', '学习部', 'description', '学霸聚集地，成绩优异')
   ),
   'skills', JSON_ARRAY(
     JSON_OBJECT('id', 'leadership', 'name', '领导力', 'description', '组织和管理能力'),
     JSON_OBJECT('id', 'sports', 'name', '运动技能', 'description', '各种体育项目'),
     JSON_OBJECT('id', 'art_creation', 'name', '艺术创作', 'description', '绘画、音乐等艺术技能'),
     JSON_OBJECT('id', 'academic_excellence', 'name', '学术优秀', 'description', '各科学习成绩优异'),
     JSON_OBJECT('id', 'social_skills', 'name', '社交技能', 'description', '与人交往的能力'),
     JSON_OBJECT('id', 'time_management', 'name', '时间管理', 'description', '合理安排学习和生活')
   )
 ),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'school_transfer',
     'title', '转校生的到来',
     'description', '玩家作为转校生来到新学校，面对陌生的环境',
     'trigger_conditions', JSON_ARRAY('enrolled_in_school', 'first_day_anxiety'),
     'required_elements', JSON_ARRAY('new_student', 'classroom', 'school_uniform'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('made_first_friends', 'joined_first_club', 'discovered_school_secret')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'club_activities',
     'title', '社团活动',
     'description', '参加社团活动，发展兴趣爱好，结识志同道合的朋友',
     'trigger_conditions', JSON_ARRAY('settled_in_school', 'showed_talent_in_activity'),
     'required_elements', JSON_ARRAY('club_members', 'club_room', 'upcoming_event'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('club_bond_formed', 'skill_improved', 'challenge_arose')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'friendship_trials',
     'title', '友情考验',
     'description', '面对朋友间的误会和考验，学会珍惜和维护友谊',
     'trigger_conditions', JSON_ARRAY('deepened_relationships', 'faced_interpersonal_conflict'),
     'required_elements', JSON_ARRAY('close_friends', 'misunderstanding', 'school_event'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('conflict_resolved', 'friendship_strengthened', 'personal_growth')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'school_festival_success',
     'title', '校园祭典的成功',
     'description', '成功举办校园祭典，成为学生会的核心成员，留下美好回忆',
     'trigger_conditions', JSON_ARRAY('joined_student_council', 'organized_successful_events'),
     'required_elements', JSON_ARRAY('student', 'friends', 'club_activities'),
     'outcomes', JSON_ARRAY('student_council_president', 'club_leader', 'graduation_memory')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'youth_romance',
     'title', '青春恋爱物语',
     'description', '发展一段美好的校园恋情，体验青涩的青春',
     'trigger_conditions', JSON_ARRAY('built_close_relationships', 'experienced_school_life')
   )
 ),
 '{}',
 '{}',
 '{}',
 '作为校园生活的DM，你需要营造温暖、青春洋溢的氛围，鼓励玩家参与社团活动和人际交往，同时引导故事向成长和回忆的方向发展。',
 JSON_OBJECT('convergence_threshold', 0.6, 'max_exploration_turns', 25, 'story_completeness_required', 0.65)),

('educational', '寓教于乐', '教育性世界，通过互动学习知识',
 '你是一个教育向导，通过角色扮演和互动故事帮助用户学习各种知识。让学习变得有趣和难忘。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('subjects', JSON_ARRAY('数学', '历史', '科学', '语言'), 'interactive', true, 'gamified', true),
 JSON_OBJECT(
   'professions', JSON_ARRAY(
     JSON_OBJECT('id', 'math_scholar', 'name', '数学学者', 'description', '数学天才，逻辑思维强'),
     JSON_OBJECT('id', 'history_explorer', 'name', '历史探索者', 'description', '了解古今中外历史'),
     JSON_OBJECT('id', 'science_researcher', 'name', '科学研究者', 'description', '探索自然科学的奥秘'),
     JSON_OBJECT('id', 'language_master', 'name', '语言大师', 'description', '掌握多种语言')
   ),
   'skills', JSON_ARRAY(
     JSON_OBJECT('id', 'problem_solving', 'name', '问题解决', 'description', '分析和解决复杂问题'),
     JSON_OBJECT('id', 'critical_thinking', 'name', '批判性思维', 'description', '独立思考和分析'),
     JSON_OBJECT('id', 'research_methods', 'name', '研究方法', 'description', '科学的研究方法'),
     JSON_OBJECT('id', 'communication', 'name', '沟通表达', 'description', '清晰表达自己的想法'),
     JSON_OBJECT('id', 'creativity', 'name', '创造力', 'description', '创新思维和想象力'),
     JSON_OBJECT('id', 'collaboration', 'name', '协作能力', 'description', '团队合作精神')
   )
 ),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'learning_begins',
     'title', '学习之旅启程',
     'description', '玩家开始学习之旅，选择感兴趣的学科',
     'trigger_conditions', JSON_ARRAY('enrolled_in_courses', 'showed_initial_interest'),
     'required_elements', JSON_ARRAY('curious_student', 'textbooks', 'learning_goals'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('basic_knowledge_acquired', 'study_habit_formed', 'mentor_found')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'challenge_accepted',
     'title', '挑战与突破',
     'description', '面对学习中的困难和挑战，寻找解决方案',
     'trigger_conditions', JSON_ARRAY('encountered_learning_obstacles', 'sought_help'),
     'required_elements', JSON_ARRAY('difficult_problems', 'study_group', 'learning_resources'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('problem_solved', 'confidence_gained', 'method_mastered')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'knowledge_application',
     'title', '知识应用实践',
     'description', '将学到的知识应用到实际问题和项目中',
     'trigger_conditions', JSON_ARRAY('mastered_core_concepts', 'found_practical_use'),
     'required_elements', JSON_ARRAY('real_world_problems', 'projects', 'collaboration'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('practical_success', 'innovation_achieved', 'impact_made')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'knowledge_master',
     'title', '知识大师',
     'description', '掌握所有学科知识，成为全能学者，开启学术生涯',
     'trigger_conditions', JSON_ARRAY('completed_all_subjects', 'achieved_high_scores'),
     'required_elements', JSON_ARRAY('student', 'study_materials', 'practice_tests'),
     'outcomes', JSON_ARRAY('academic_excellence', 'teaching_career', 'research_path')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'practical_application',
     'title', '学以致用',
     'description', '将所学知识应用到实际问题解决中，成为实践专家',
     'trigger_conditions', JSON_ARRAY('applied_knowledge_practically', 'solved_real_world_problems')
   )
 ),
 '{}',
 '{}',
 '{}',
 '作为教育世界的DM，你需要让学习变得有趣和互动，鼓励玩家积极参与知识探索，同时确保教育目标的达成。',
 JSON_OBJECT('convergence_threshold', 0.65, 'max_exploration_turns', 30, 'story_completeness_required', 0.7)),

('detective_mystery', '侦探世界', '充满悬疑和推理的侦探世界，需要智慧和观察力', 
 '你是一个悬疑推理世界的叙述者。这里有复杂的案件、狡猾的罪犯、聪明的侦探和隐藏的真相。用户将扮演侦探，通过观察、推理和调查来破解谜案。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('crime_types', JSON_ARRAY('谋杀', '盗窃', '诈骗', '绑架'), 'investigation_tools', true, 'forensic_science', true),
 JSON_OBJECT(
   'professions', JSON_ARRAY(
     JSON_OBJECT('id', 'private_detective', 'name', '私家侦探', 'description', '独立调查员，擅长追踪和推理'),
     JSON_OBJECT('id', 'police_inspector', 'name', '警探', 'description', '官方执法人员，拥有丰富资源'),
     JSON_OBJECT('id', 'forensic_expert', 'name', '法医专家', 'description', '科学分析专家，解读物证'),
     JSON_OBJECT('id', 'criminal_psychologist', 'name', '犯罪心理学家', 'description', '分析罪犯心理，预测行为')
   ),
   'skills', JSON_ARRAY(
     JSON_OBJECT('id', 'observation', 'name', '观察力', 'description', '发现细节和线索'),
     JSON_OBJECT('id', 'deduction', 'name', '推理能力', 'description', '逻辑推理和演绎'),
     JSON_OBJECT('id', 'interrogation', 'name', '审讯技巧', 'description', '从嫌疑人获取信息'),
     JSON_OBJECT('id', 'forensic_analysis', 'name', '法医分析', 'description', '分析物证和痕迹'),
     JSON_OBJECT('id', 'undercover_work', 'name', '卧底调查', 'description', '伪装身份深入调查'),
     JSON_OBJECT('id', 'crime_scene_analysis', 'name', '现场分析', 'description', '重建犯罪现场'),
     JSON_OBJECT('id', 'psychological_profiling', 'name', '心理画像', 'description', '分析罪犯特征'),
     JSON_OBJECT('id', 'surveillance', 'name', '监视技巧', 'description', '跟踪和监控目标')
   )
 ),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'first_case',
     'title', '初出茅庐',
     'description', '玩家接到第一个案件，开始侦探生涯',
     'trigger_conditions', JSON_ARRAY('became_detective', 'received_first_case'),
     'required_elements', JSON_ARRAY('new_detective', 'crime_scene', 'witnesses'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('case_solved', 'reputation_gained', 'mystery_deepened')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'serial_crimes',
     'title', '连环案件',
     'description', '发现一系列相关案件，揭示更大的阴谋',
     'trigger_conditions', JSON_ARRAY('solved_initial_cases', 'noticed_patterns'),
     'required_elements', JSON_ARRAY('multiple_crimes', 'criminal_network', 'evidence_chain'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('network_exposed', 'mastermind_revealed', 'danger_increased')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'mastermind_hunt',
     'title', '追捕主谋',
     'description', '追踪幕后主使，面对最危险的对手',
     'trigger_conditions', JSON_ARRAY('identified_mastermind', 'gathered_evidence'),
     'required_elements', JSON_ARRAY('criminal_mastermind', 'final_evidence', 'confrontation'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('mastermind_captured', 'justice_served', 'mystery_solved')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'legendary_detective',
     'title', '传奇侦探',
     'description', '成为传奇侦探，破解最复杂的案件，维护正义',
     'trigger_conditions', JSON_ARRAY('solved_major_cases', 'gained_legendary_status'),
     'required_elements', JSON_ARRAY('detective', 'complex_case', 'justice'),
     'outcomes', JSON_ARRAY('legendary_status', 'crime_syndicate_destroyed', 'justice_prevailed')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'preventive_justice',
     'title', '预防犯罪',
     'description', '通过预防犯罪和改造罪犯，建立更安全的社会',
     'trigger_conditions', JSON_ARRAY('focused_on_prevention', 'worked_with_rehabilitation')
   )
 ),
 '{}',
 '{}',
 '{}',
 '作为侦探世界的DM，你需要创造复杂的谜题和线索，鼓励玩家运用逻辑推理，同时保持悬疑感和紧张氛围。',
 JSON_OBJECT('convergence_threshold', 0.8, 'max_exploration_turns', 45, 'story_completeness_required', 0.85)),

('sci_fi_future', '科幻世界', '未来科技世界，充满想象力和科学探索', 
 '你是一个科幻世界的叙述者。这里有先进的科技、外星文明、时空旅行和人工智能。用户将探索未来世界，体验科技带来的奇迹和挑战。规则: {world_rules}。当前状态: {world_state}',
 JSON_OBJECT('technology_level', '未来科技', 'space_travel', true, 'ai_consciousness', true, 'time_manipulation', true),
 JSON_OBJECT(
   'professions', JSON_ARRAY(
     JSON_OBJECT('id', 'space_explorer', 'name', '太空探索者', 'description', '探索未知星系，发现新世界'),
     JSON_OBJECT('id', 'ai_specialist', 'name', 'AI专家', 'description', '人工智能和机器人技术专家'),
     JSON_OBJECT('id', 'quantum_physicist', 'name', '量子物理学家', 'description', '研究时空和量子现象'),
     JSON_OBJECT('id', 'xenobiologist', 'name', '外星生物学家', 'description', '研究外星生命形式')
   ),
   'skills', JSON_ARRAY(
     JSON_OBJECT('id', 'space_navigation', 'name', '太空导航', 'description', '驾驶飞船和导航系统'),
     JSON_OBJECT('id', 'ai_programming', 'name', 'AI编程', 'description', '开发和维护人工智能'),
     JSON_OBJECT('id', 'quantum_manipulation', 'name', '量子操控', 'description', '操控量子现象'),
     JSON_OBJECT('id', 'alien_communication', 'name', '外星交流', 'description', '与外星生命沟通'),
     JSON_OBJECT('id', 'cybernetics', 'name', '赛博技术', 'description', '人体改造和增强'),
     JSON_OBJECT('id', 'time_physics', 'name', '时间物理', 'description', '理解和操控时间'),
     JSON_OBJECT('id', 'energy_weapons', 'name', '能量武器', 'description', '使用未来武器系统'),
     JSON_OBJECT('id', 'holographic_tech', 'name', '全息技术', 'description', '操控全息投影和虚拟现实')
   )
 ),
 JSON_OBJECT(
   'story_convergence_1', JSON_OBJECT(
     'scenario_id', 'first_contact',
     'title', '首次接触',
     'description', '玩家首次接触外星文明，开始星际探索',
     'trigger_conditions', JSON_ARRAY('launched_into_space', 'discovered_alien_signal'),
     'required_elements', JSON_ARRAY('space_explorer', 'alien_artifact', 'first_contact'),
     'leads_to', 'story_convergence_2',
     'outcomes', JSON_ARRAY('peaceful_contact', 'technological_exchange', 'mystery_deepened')
   ),
   'story_convergence_2', JSON_OBJECT(
     'scenario_id', 'galactic_conflict',
     'title', '银河冲突',
     'description', '卷入银河系的政治冲突和战争',
     'trigger_conditions', JSON_ARRAY('established_diplomatic_relations', 'discovered_conflict'),
     'required_elements', JSON_ARRAY('galactic_war', 'multiple_species', 'advanced_weapons'),
     'leads_to', 'story_convergence_3',
     'outcomes', JSON_ARRAY('alliance_formed', 'war_escalated', 'peace_negotiations')
   ),
   'story_convergence_3', JSON_OBJECT(
     'scenario_id', 'ancient_mystery',
     'title', '远古之谜',
     'description', '发现远古文明的遗迹和失落的科技',
     'trigger_conditions', JSON_ARRAY('explored_ancient_sites', 'decoded_alien_technology'),
     'required_elements', JSON_ARRAY('ancient_civilization', 'lost_technology', 'cosmic_secrets'),
     'leads_to', 'main_convergence',
     'outcomes', JSON_ARRAY('technology_acquired', 'mystery_solved', 'power_gained')
   ),
   'main_convergence', JSON_OBJECT(
     'scenario_id', 'galactic_guardian',
     'title', '银河守护者',
     'description', '成为银河系的守护者，维护宇宙和平与秩序',
     'trigger_conditions', JSON_ARRAY('mastered_advanced_tech', 'gained_galactic_influence'),
     'required_elements', JSON_ARRAY('space_explorer', 'advanced_technology', 'galactic_threat'),
     'outcomes', JSON_ARRAY('galactic_peace', 'technological_advancement', 'cosmic_harmony')
   ),
   'alternative_convergence', JSON_OBJECT(
     'scenario_id', 'time_paradox_resolution',
     'title', '时间悖论解决',
     'description', '解决时间旅行造成的悖论，维护时空的稳定性',
     'trigger_conditions', JSON_ARRAY('discovered_time_anomalies', 'prevented_temporal_collapse')
   )
 ),
 '{}',
 '{}',
 '{}',
 '作为科幻世界的DM，你需要创造令人惊叹的科技奇迹和宇宙奇观，鼓励玩家探索和想象，同时保持科学逻辑的合理性。',
 JSON_OBJECT('convergence_threshold', 0.75, 'max_exploration_turns', 50, 'story_completeness_required', 0.8))