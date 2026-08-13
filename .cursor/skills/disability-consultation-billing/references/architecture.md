# 推奨アーキテクチャ

Browser -> Next.js -> Spring Boot REST API -> PostgreSQL
                                    |-> Billing Calculation Engine
                                    |-> Validation Engine
                                    |-> Excel Importer
                                    |-> Kokuho Export Adapter
                                    |-> Audit Service

国保連への送信は開発システムから直接行わず、生成ファイル -> 国提供の取込送信システム -> 国保連を基本運用とする。
