FROM ruby:3.3.3-slim

# Instala dependências do sistema (essenciais para pg e puma/rails)
RUN apt-get update -qq && apt-get install -y build-essential libpq-dev nodejs

WORKDIR /app

# Copia arquivos de dependências primeiro (otimiza o cache do Docker)
COPY Gemfile Gemfile.lock ./
RUN bundle install

COPY . .

EXPOSE 3000
